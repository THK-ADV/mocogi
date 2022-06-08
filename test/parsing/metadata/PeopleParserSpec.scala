package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.metadata.PeopleParser.{peopleFileParser, personParser}
import parsing.types.People
import parsing.{ParserSpecHelper, withResFile}

class PeopleParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A People Parser" when {
    "parse people file" should {
      "parse a single person" in {
        val input =
          """tbb:
            |  lastname: foo
            |  firstname: bar
            |  title: bar. baz.
            |  faculty: b""".stripMargin
        val (res1, rest1) = peopleFileParser.run(input)
        assert(
          res1.value == List(People("tbb", "foo", "bar", "bar. baz.", "b"))
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
        val (res1, rest1) = peopleFileParser.run(input)
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

      "parse all people in people-all.yaml" in {
        val (res1, rest1) = withResFile("people-all.yaml")(peopleFileParser.run)
        assert(res1.value.size == 5)
        assert(rest1.isEmpty)
      }
    }

    "parse single people" should {
      "return a person if the input is simple" in {
        val input = "person.ald\n"
        val (res1, rest1) = personParser.run(input)
        assert(
          res1.value == List(
            People("ald", "Dobrynin", "Alexander", "M.Sc.", "F10")
          )
        )
        assert(rest1.isEmpty)
      }

      "return a person and the remaining input if it's not a valid person" in {
        val input = "person.ald\n abc"
        val (res3, rest3) = personParser.run(input)
        assert(
          res3.value == List(
            People("ald", "Dobrynin", "Alexander", "M.Sc.", "F10")
          )
        )
        assert(rest3 == " abc")
      }

      "not return a person if they are unknown" in {
        val input = "person.abc\n"
        val (res4, rest4) = personParser.run(input)
        assert(res4.value.isEmpty)
        assert(rest4 == "person.abc\n")
      }
    }

    "parse multiple persons seperated by dashes" should {

      "return 2 persons which are only seperated by dashes" in {
        val input =
          """-person.ald
            |-person.abe
            |""".stripMargin
        val (res1, rest1) = personParser.run(input)
        assert(
          res1.value == List(
            People("ald", "Dobrynin", "Alexander", "M.Sc.", "F10"),
            People("abe", "Bertels", "Anja", "B.Sc.", "F10")
          )
        )
        assert(rest1.isEmpty)
      }

      "return 2 persons which are seperated by dashes and contain random whitespace in between" in {
        val input =
          """  -  person.ald
            |  -  person.abe
            |""".stripMargin
        val (res2, rest2) =
          personParser.run(input)
        assert(
          res2.value == List(
            People("ald", "Dobrynin", "Alexander", "M.Sc.", "F10"),
            People("abe", "Bertels", "Anja", "B.Sc.", "F10")
          )
        )
        assert(rest2.isEmpty)
      }

      "return 2 persons and the remaining input which can't be parsed" in {
        val input =
          """  -  person.ald
            |  -  person.abe
            | abc""".stripMargin
        val (res3, rest3) =
          personParser.run(input)
        assert(
          res3.value == List(
            People("ald", "Dobrynin", "Alexander", "M.Sc.", "F10"),
            People("abe", "Bertels", "Anja", "B.Sc.", "F10")
          )
        )
        assert(rest3 == " abc")
      }

      "skip persons which are unknown and the return the known ones" in {
        val input =
          """  -  person.ald
            |  -  person.abe
            | -  person.abc
            |""".stripMargin
        val (res4, rest4) = personParser.run(
          input
        )
        assert(
          res4.value == List(
            People("ald", "Dobrynin", "Alexander", "M.Sc.", "F10"),
            People("abe", "Bertels", "Anja", "B.Sc.", "F10")
          )
        )
        assert(rest4 == " -  person.abc\n")
      }
    }
  }
}
