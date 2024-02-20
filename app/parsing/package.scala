import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.annotation.tailrec
import scala.io.Source
import scala.util.Try

package object parsing {
  import parser.Parser
  import parser.Parser._
  import parser.ParserOps._

  def removeIndentation(level: Int = 1): Parser[Unit] = Parser { input =>
    val indentations = level * 2

    @tailrec
    def go(str: String, soFar: String): String = {
      val currentLine = str.takeWhile(_ != '\n')
      val leadingSpaces = currentLine.takeWhile(_ == ' ')
      val newLine =
        if (leadingSpaces.isEmpty) currentLine
        else currentLine.drop(indentations min leadingSpaces.length)
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
      .map(_.trim)

  def doubleForKey(key: String): Parser[Double] =
    keyParser(key)
      .take(double)

  def intForKey(key: String): Parser[Int] =
    keyParser(key)
      .take(int)

  def posIntForKey(key: String): Parser[Int] =
    intForKey(key).flatMap(i =>
      if (i >= 0) always(i) else never("int to be positive")
    )

  def dateForKey(key: String): Parser[LocalDate] =
    singleLineStringForKey(key)
      .flatMap(localDateParser)

  sealed trait MultilineStringStrategy
  case object > extends MultilineStringStrategy
  case object | extends MultilineStringStrategy
  case object Plain extends MultilineStringStrategy

  def multilineStringStrategy: Parser[MultilineStringStrategy] =
    (prefixUntil("\n") or rest)
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
      val leadingSpaces = right.takeWhile(_ == ' ')
      val rightWithoutSpaces = right.stripPrefix(leadingSpaces)
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
    val values = shiftSpaces(t._2)
    var pointer = 1
    val str = values.foldLeft("") { case (acc, str) =>
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

  def withFile0[A](path: String)(input: String => A): A = {
    val s = Source.fromFile(new File(path))
    val res = input(s.mkString)
    s.close()
    res
  }

  def singleValueParser[A](key: String, optionPrefix: A => String)(implicit
      options: Seq[A]
  ): Parser[A] =
    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .take(
        oneOf(
          options.map(o =>
            prefix(optionPrefix(o))
              .map(_ => o)
          ): _*
        )
      )

  def multipleValueParser[A](
      key: String,
      singleParser: Parser[A],
      minimum: Int = 0
  ): Parser[List[A]] = {
    val dashes =
      zeroOrMoreSpaces
        .skip(prefix("-"))
        .skip(zeroOrMoreSpaces)
        .take(singleParser)
        .many(minimum = minimum)

    prefix(s"$key:")
      .skip(zeroOrMoreSpaces)
      .skip(optional(newline))
      .take(singleParser.map(a => List(a)) or dashes)
  }

  def multipleValueParser[A](
      key: String,
      optionPrefix: A => String,
      minimum: Int
  )(implicit
      options: Seq[A]
  ): Parser[List[A]] = multipleValueParser(
    key,
    oneOf(
      options.map(o =>
        prefix(optionPrefix(o))
          .map(_ => o)
      ): _*
    ),
    minimum
  )

  def singleValueRawParser(key: String, prefix: String): Parser[String] =
    keyParser(key)
      .skip(Parser.prefix(prefix))
      .take(prefixTo("\n").or(rest))
      .map(_.trim)

  def multipleValueRawParser(
      key: String,
      prefix: String
  ): Parser[List[String]] =
    keyParser(key)
      .take(
        skipFirst(zeroOrMoreSpaces)
          .skip(Parser.prefix("-"))
          .skip(zeroOrMoreSpaces)
          .take(
            skipFirst(Parser.prefix(prefix))
              .take(prefixTo("\n").or(rest))
              .map(_.trim)
          )
          .many()
      )

  private val localDatePattern = DateTimeFormatter.ofPattern("dd.MM.yyyy")

  def localDateParser(string: String): Parser[LocalDate] =
    localDateParser(string, localDatePattern)

  def localDateParser(
      string: String,
      pattern: DateTimeFormatter
  ): Parser[LocalDate] =
    try always(LocalDate.parse(string, pattern))
    catch {
      case t: Throwable =>
        never(s"date with format $pattern. error: ${t.getMessage}")
    }

  def singleLineCommentParser(): Parser[Unit] =
    prefix("#").skip(prefix(_ != '\n').skip(newline))

  def uuidParser(string: String): Parser[UUID] =
    Try(UUID.fromString(string)).fold(_ => never("uuid"), always)
}
