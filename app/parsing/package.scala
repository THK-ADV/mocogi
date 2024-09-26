import java.io.File
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.util.UUID

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.Try

import cats.data.NonEmptyList
import cats.syntax.either._
import io.circe.ACursor
import io.circe.Decoder
import io.circe.HCursor

package object parsing {
  import parser.Parser
  import parser.Parser._
  import parser.ParserOps._

  implicit class ParserListOps[A](private val parser: Parser[List[A]]) {
    def nel(): Parser[NonEmptyList[A]] =
      parser.flatMap(xs =>
        if (xs.isEmpty) never("one entry")
        else always(NonEmptyList.fromListUnsafe(xs))
      )
  }

  implicit class CursorOps(private val self: ACursor) extends AnyVal {
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

  def removeIndentation(level: Int = 1): Parser[Unit] = Parser { input =>
    val indentations = level * 2

    @tailrec
    def go(str: String, soFar: String): String = {
      val currentLine   = str.takeWhile(_ != '\n')
      val leadingSpaces = currentLine.takeWhile(_ == ' ')
      val newLine =
        if (leadingSpaces.isEmpty) currentLine
        else currentLine.drop(indentations.min(leadingSpaces.length))
      val nextStr = str.drop(currentLine.length + 1)
      if (nextStr.isEmpty) soFar + newLine
      else go(nextStr, soFar + newLine + "\n")
    }

    Right(()) -> go(input, "")
  }

  def keyParser(key: String): Parser[Unit] =
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

  def shiftSpaces(xs: List[String]): List[String] = {
    def go(left: String, right: String): (String, String) = {
      val leadingSpaces            = right.takeWhile(_ == ' ')
      val rightWithoutSpaces       = right.stripPrefix(leadingSpaces)
      val leftWithTrailingNewlines = left + ("\n" * leadingSpaces.length)
      (leftWithTrailingNewlines, rightWithoutSpaces)
    }

    xs.size match {
      case 0 | 1 => xs
      case 2 =>
        val (l, r) = go(xs.head, xs.last)
        List(l, r)
      case _ =>
        val (l, r) = go(xs.head, xs(1))
        l :: shiftSpaces(r :: xs.drop(2))
    }
  }

  private def mergeMultilineString(
      t: (MultilineStringStrategy, List[String])
  ): String = {
    val strategy = t._1
    val values   = shiftSpaces(t._2)
    var pointer  = 1
    val str = values.foldLeft("") {
      case (acc, str) =>
        val str0 = strategy match {
          case > if !str.endsWith("\n") && pointer != values.size =>
            str + " "
          case Plain if !str.endsWith("\n") && pointer != values.size =>
            str + " "
          case | if pointer != values.size => str + '\n'
          case _                           => str
        }
        pointer += 1
        acc + str0
    }

    strategy match {
      case >     => str + '\n'
      case |     => str + '\n'
      case Plain => str
    }
  }

  def multilineStringForKey(key: String): Parser[String] =
    skipFirst(prefix(s"$key:"))
      .take(multilineStringStrategy)
      .skip(newline)
      .zip(
        whitespace
          .skip(whitespace)
          .take(prefix(_ != '\n'))
          .many(newline)
      )
      .map(mergeMultilineString)
      .map(_.stripLeading())

  def stringForKey(key: String): Parser[String] =
    oneOf(
      multilineStringForKey(key),
      singleLineStringForKey(key)
    )

  implicit def decoderList[A](implicit decoder: Decoder[A]): Decoder[List[A]] =
    (c: HCursor) => {
      val builder = ListBuffer.empty[A]
      c.keys.foreach(
        _.foreach(key =>
          c.get[A](key) match {
            case Left(value) =>
              return Decoder.failed(value)
            case Right(value) =>
              builder += value
          }
        )
      )
      Right(builder.result())
    }

  def withFile0[A](path: String)(input: String => A): A = {
    val s   = Source.fromFile(new File(path))
    val res = input(s.mkString)
    s.close()
    res
  }

  def multipleValueParser[A](
      key: String,
      singleParser: Parser[A]
  ): Parser[List[A]] = {
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

  def multipleValueParser[A](
      key: String,
      optionPrefix: A => String
  )(implicit options: Seq[A]): Parser[List[A]] = multipleValueParser(
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

  def multipleValueRawParser(
      key: String,
      prefix: String
  ): Parser[List[String]] = {
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

  private val localDatePattern = DateTimeFormatter.ofPattern("dd.MM.yyyy")

  implicit def localDateDecoder: Decoder[LocalDate] =
    Decoder.decodeString.emap { str =>
      Either
        .catchNonFatal(LocalDate.parse(str, localDatePattern))
        .left
        .map(_.getMessage)
    }

  def uuidParser(string: String): Parser[UUID] =
    Try(UUID.fromString(string)).fold(_ => never("uuid"), always)
}
