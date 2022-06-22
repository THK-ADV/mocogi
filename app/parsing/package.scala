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

  def pprint(
      obj: Any,
      depth: Int = 0,
      paramName: Option[String] = None
  ): Unit = {
    val indent = "  " * depth
    val prettyName = paramName.fold("")(x => s"$x: ")
    val ptype = obj match {
      case _: Iterable[Any] => ""
      case obj: Product     => obj.productPrefix
      case _                => obj.toString
    }

    println(s"$indent$prettyName$ptype")

    obj match {
      case seq: Iterable[Any] =>
        seq.foreach(pprint(_, depth + 1))
      case obj: Product =>
        (obj.productIterator zip obj.productElementNames)
          .foreach { case (subObj, paramName) =>
            pprint(subObj, depth + 1, Some(paramName))
          }
      case _ =>
    }
  }
}
