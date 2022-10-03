package parsing.base

import basedata.Person
import helper.FakeApplication
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.base.PersonFileParser
import parsing.{ParserSpecHelper, withFile0}

final class PersonFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication {

  val parser = app.injector.instanceOf(classOf[PersonFileParser]).fileParser

  "A Person File Parser" should {
    "parse a single person" in {
      val input =
        """tbb:
          |  lastname: foo
          |  firstname: bar
          |  title: bar. baz.
          |  faculty: b""".stripMargin
      val (res1, rest1) = parser.parse(input)
      assert(
        res1.value == List(Person("tbb", "foo", "bar", "bar. baz.", "b"))
      )
      assert(rest1.isEmpty)
    }

    "parse all people" in {
      val input =
        """all:
          |  lastname: Alle
          |  firstname: Alle
          |  title: Alle
          |  faculty: Alle
          |nn:
          |  lastname: N.N.
          |  firstname: N.N.
          |  title: N.N.
          |  faculty: N.N.
          |ald:
          |  lastname: Dobrynin
          |  firstname: Alexander
          |  title: M.Sc.
          |  faculty: F10
          |abe:
          |  lastname: Bertels
          |  firstname: Anja
          |  title: B.Sc.
          |  faculty: F10
          |ddu:
          |  lastname: Dubbert
          |  firstname: Dennis
          |  title: M.Sc.
          |  faculty: F10""".stripMargin
      val (res1, rest1) = parser.parse(input)
      assert(
        res1.value.map(_.abbrev) == List(
          "all",
          "nn",
          "ald",
          "abe",
          "ddu"
        )
      )
      assert(rest1.isEmpty)
    }

    "parse all people in person.yaml" in {
      val (res1, rest1) =
        withFile0("test/parsing/res/person.yaml")(parser.parse)
      assert(res1.value.size == 5)
      assert(rest1.isEmpty)
    }
  }
}
