package parsing

import org.scalatest.wordspec.AnyWordSpec
import parser.Parser

trait ParserSpecHelper { self: AnyWordSpec =>

  def assertError[A](
      p: Parser[A],
      input: String,
      expected: String,
      remainingInput: Option[String] = None
  ) = {
    val (res, rest) = p.parse(input)
    assert(rest == input)
    res match {
      case Right(_) => fail()
      case Left(e) =>
        assert(e.expected == expected)
        assert(e.found == remainingInput.getOrElse(input))
    }
  }
}
