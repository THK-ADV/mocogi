import java.io.File
import scala.io.Source

package object parsing {
  import parser.Parser
  import parser.Parser._
  import parser.ParserOps._

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

  def withFile0[A](path: String)(input: String => A): A = {
    val s = Source.fromFile(new File(path))
    val res = input(s.mkString)
    s.close()
    res
  }
}
