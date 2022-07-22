import java.io.File
import scala.io.Source

package object parsing {
  import parser.Parser
  import parser.Parser._
  import parser.ParserOps._

  def stringForKey(key: String): Parser[String] =
    skipFirst(prefix(s"$key:"))
      .skip(zeroOrMoreSpaces)
      .take(prefixTo("\n").or(rest))
      .map(_.trim)

  def doubleForKey(key: String): Parser[Double] =
    skipFirst(prefix(s"$key:"))
      .skip(zeroOrMoreSpaces)
      .take(double)

  def intForKey(key: String): Parser[Int] =
    skipFirst(prefix(s"$key:"))
      .skip(zeroOrMoreSpaces)
      .take(int)

  def withFile0[A](path: String)(input: String => A): A = {
    val s = Source.fromFile(new File(path))
    val res = input(s.mkString)
    s.close()
    res
  }
}
