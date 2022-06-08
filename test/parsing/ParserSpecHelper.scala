package parsing

import org.scalatest.wordspec.AnyWordSpec
import parser.Parser

trait ParserSpecHelper { self: AnyWordSpec =>

  def withTestFile[A](name: String)(input: String => A): A =
    withFile("test/parsing/res")(name)(input)

  def assertError[A](
      p: Parser[A],
      input: String,
      expected: String,
      remainingInput: Option[String] = None
  ) = {
    val (res, rest) = p.run(input)
    assert(rest == input)
    res match {
      case Right(_) => fail()
      case Left(e) =>
        assert(e.expected == expected)
        assert(e.remainingInput == remainingInput.getOrElse(input))
    }
  }
}
