import java.io.File
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.util.UUID

import scala.io.Source
import scala.util.Try

import cats.data.NonEmptyList
import cats.implicits.*
import io.circe.ACursor
import io.circe.Decoder
import io.circe.HCursor

package object parsing {
  import parser.Parser
  import parser.Parser.*
  import parser.ParserOps.*

  extension [A](self: Parser[List[A]]) {
    def nel(): Parser[NonEmptyList[A]] =
      self.flatMap(xs =>
        if (xs.isEmpty) never("one entry")
        else always(NonEmptyList.fromListUnsafe(xs))
      )
  }

  extension (self: ACursor) {
    def getNonEmptyList(key: String): Decoder.Result[NonEmptyList[String]] = {
      val field = self.downField(key)
      if (field.downArray.succeeded) {
        field.as[NonEmptyList[String]]
      } else {
        field.as[String].map(NonEmptyList.one)
      }
    }

    def getList(key: String): Decoder.Result[List[String]] = {
      val field = self.downField(key)
      if (field.downArray.succeeded) {
        // Parsing to List[String] results to an empty list. Thus, Seq[String] is used as a workaround
        field.as[Seq[String]].map(_.toList)
      } else {
        field.as[String].map(List(_))
      }
    }
  }

  private def keyParser(key: String): Parser[Unit] =
    skipFirst(prefix(s"$key:"))
      .skip(zeroOrMoreSpaces)

  def singleLineStringForKey(key: String): Parser[String] =
    keyParser(key)
      .take(prefixTo("\n").or(rest))
      .map { s =>
        val s0 = s.trim
        if (s0 == "''") "" else s0
      }

  def doubleForKey(key: String): Parser[Double] =
    keyParser(key)
      .take(double)

  def intForKey(key: String): Parser[Int] =
    keyParser(key)
      .take(int)

  def posIntForKey(key: String): Parser[Int] =
    intForKey(key).flatMap(i => if (i >= 0) always(i) else never("int to be positive"))

  sealed trait MultilineStringStrategy
  case object >     extends MultilineStringStrategy
  case object |     extends MultilineStringStrategy
  case object Plain extends MultilineStringStrategy

  def multilineStringStrategy: Parser[MultilineStringStrategy] =
    prefixUntil("\n")
      .or(rest)
      .flatMap { str =>
        str.trim match {
          case ">" => always(>)
          case "|" => always(|)
          case ""  => always(Plain)
          case _   => never("'>' or '|' or space or newline")
        }
      }

  private def normalizeMultilineLines(lines: List[String]): List[String] =
    lines
      .dropWhile(_.trim.isEmpty)
      .reverse
      .dropWhile(_.trim.isEmpty)
      .reverse
      .map(_.trim)

  private def foldMultilineParagraphs(lines: List[String]): String = {
    val (paragraphs, currentParagraph) =
      lines.foldLeft((List.empty[String], List.empty[String])) {
        case ((paragraphs, currentParagraph), line) if line.nonEmpty =>
          (paragraphs, currentParagraph :+ line)
        case ((paragraphs, currentParagraph), _) if currentParagraph.nonEmpty =>
          (paragraphs :+ currentParagraph.mkString(" "), Nil)
        case (state, _) =>
          state
      }

    val allParagraphs =
      if (currentParagraph.nonEmpty) paragraphs :+ currentParagraph.mkString(" ")
      else paragraphs

    allParagraphs.mkString("\n")
  }

  private def mergeMultilineString(t: (MultilineStringStrategy, List[String])): String = {
    val strategy = t._1
    val lines    = normalizeMultilineLines(t._2)
    val merged   = strategy match {
      case |     => lines.mkString("\n")
      case >     => foldMultilineParagraphs(lines)
      case Plain => foldMultilineParagraphs(lines)
    }

    strategy match {
      case > if merged.nonEmpty => merged + '\n'
      case | if merged.nonEmpty => merged + '\n'
      case _                    => merged
    }
  }

  @annotation.tailrec
  private def collectMultilineLines(
      input: String,
      blockIndent: Option[Int] = None,
      acc: List[String] = Nil
  ): (List[String], String) = {
    if (input.isEmpty) {
      (acc.reverse, "")
    } else {
      val newLineIndex = input.indexOf('\n')
      val (line, rest) =
        if (newLineIndex == -1) (input, "")
        else (input.take(newLineIndex), input.drop(newLineIndex + 1))

      if (line.trim.isEmpty) {
        collectMultilineLines(rest, blockIndent, "" :: acc)
      } else {
        val lineIndent = line.takeWhile(_.isWhitespace).length

        blockIndent match {
          case None if lineIndent > 0 =>
            collectMultilineLines(rest, Some(lineIndent), line.drop(lineIndent) :: acc)
          case Some(requiredIndent) if lineIndent >= requiredIndent =>
            collectMultilineLines(rest, blockIndent, line.drop(requiredIndent) :: acc)
          case _ =>
            (acc.reverse, input)
        }
      }
    }
  }

  def multilineStringForKey(key: String): Parser[String] =
    Parser { input =>
      val headerParser =
        skipFirst(prefix(s"$key:"))
          .take(multilineStringStrategy)
          .skip(newline)

      val (header, afterHeader) = headerParser.parse(input)
      header match {
        case Left(err) =>
          (Left(err), input)
        case Right(strategy) =>
          val (lines, rest) = collectMultilineLines(afterHeader)
          (Right(mergeMultilineString(strategy -> lines)), rest)
      }
    }

  def stringForKey(key: String): Parser[String] =
    oneOf(
      multilineStringForKey(key),
      singleLineStringForKey(key)
    )

  given decoderList[A](using Decoder[A]): Decoder[List[A]] =
    (c: HCursor) => {
      c.keys match {
        case Some(keys) =>
          keys.toList.traverse(key => c.get[A](key))
        case None =>
          Right(List.empty[A])
      }
    }

  def withFile0[A](path: String)(input: String => A): A = {
    val s   = Source.fromFile(new File(path))
    val res = input(s.mkString)
    s.close()
    res
  }

  def multipleValueParser[A](key: String, singleParser: Parser[A]): Parser[List[A]] = {
    val dashes =
      zeroOrMoreSpaces
        .skip(prefix("-"))
        .skip(zeroOrMoreSpaces)
        .take(singleParser)
        .many()

    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .take(singleParser.map(a => List(a)).or(dashes))
  }

  def multipleValueParser[A](key: String, optionPrefix: A => String)(implicit options: Seq[A]): Parser[List[A]] =
    multipleValueParser(
      key,
      oneOf(
        options.map(o =>
          prefix(optionPrefix(o))
            .map(_ => o)
        )*
      )
    )

  def singleValueRawParser(key: String, prefix: String): Parser[String] =
    keyParser(key)
      .skip(Parser.prefix(prefix))
      .take(prefixTo("\n").or(rest))
      .map(_.trim)

  def multipleValueRawParser(key: String, prefix: String): Parser[List[String]] = {
    val single =
      skipFirst(Parser.prefix(prefix))
        .take(prefixTo("\n").or(rest))
        .map(_.trim)

    val dashes =
      skipFirst(zeroOrMoreSpaces)
        .skip(Parser.prefix("-"))
        .skip(zeroOrMoreSpaces)
        .take(single)
        .many()

    keyParser(key)
      .take(single.map(a => List(a)).or(dashes))
  }

  given localDateDecoder: Decoder[LocalDate] =
    Decoder.decodeString.emap { str =>
      Either
        .catchNonFatal(LocalDate.parse(str, DateTimeFormatter.ofPattern("dd.MM.yyyy")))
        .left
        .map(_.getMessage)
    }

  def uuidParser(string: String): Parser[UUID] =
    Try(UUID.fromString(string)).fold(_ => never("uuid"), always)
}
