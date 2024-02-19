package parsing.metadata

import helper.{FakeApplication, FakeIdentities}
import models.core.{Identity, PersonStatus}
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.ParserSpecHelper

class IdentityParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication
    with FakeIdentities {

  val parser =
    app.injector.instanceOf(classOf[IdentityParser]).parser

  "A People Parser" when {
    "parse single people" should {
      "return a person if the input is simple" in {
        val input = "person.ald\n"
        val (res1, rest1) = parser.parse(input)
        assert(
          res1.value == List(
            Identity.Person(
              "ald",
              "Dobrynin",
              "Alexander",
              "M.Sc.",
              List(f10),
              "ad",
              "ald",
              PersonStatus.Active
            )
          )
        )
        assert(rest1.isEmpty)
      }

      "return a person and the remaining input if it's not a valid person" in {
        val input = "person.ald\n abc"
        val (res3, rest3) = parser.parse(input)
        assert(
          res3.value == List(
            Identity.Person(
              "ald",
              "Dobrynin",
              "Alexander",
              "M.Sc.",
              List(f10),
              "ad",
              "ald",
              PersonStatus.Active
            )
          )
        )
        assert(rest3 == " abc")
      }

      "not return a person if they are unknown" in {
        val input = "person.abc\n"
        val (res4, rest4) = parser.parse(input)
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
        val (res1, rest1) = parser.parse(input)
        assert(
          res1.value == List(
            Identity.Person(
              "ald",
              "Dobrynin",
              "Alexander",
              "M.Sc.",
              List(f10),
              "ad",
              "ald",
              PersonStatus.Active
            ),
            Identity.Person(
              "abe",
              "Bertels",
              "Anja",
              "B.Sc.",
              List(f10),
              "ab",
              "abe",
              PersonStatus.Active
            )
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
          parser.parse(input)
        assert(
          res2.value == List(
            Identity.Person(
              "ald",
              "Dobrynin",
              "Alexander",
              "M.Sc.",
              List(f10),
              "ad",
              "ald",
              PersonStatus.Active
            ),
            Identity.Person(
              "abe",
              "Bertels",
              "Anja",
              "B.Sc.",
              List(f10),
              "ab",
              "abe",
              PersonStatus.Active
            )
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
          parser.parse(input)
        assert(
          res3.value == List(
            Identity.Person(
              "ald",
              "Dobrynin",
              "Alexander",
              "M.Sc.",
              List(f10),
              "ad",
              "ald",
              PersonStatus.Active
            ),
            Identity.Person(
              "abe",
              "Bertels",
              "Anja",
              "B.Sc.",
              List(f10),
              "ab",
              "abe",
              PersonStatus.Active
            )
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
        val (res4, rest4) = parser.parse(
          input
        )
        assert(
          res4.value == List(
            Identity.Person(
              "ald",
              "Dobrynin",
              "Alexander",
              "M.Sc.",
              List(f10),
              "ad",
              "ald",
              PersonStatus.Active
            ),
            Identity.Person(
              "abe",
              "Bertels",
              "Anja",
              "B.Sc.",
              List(f10),
              "ab",
              "abe",
              PersonStatus.Active
            )
          )
        )
        assert(rest4 == " -  person.abc\n")
      }
    }
  }
}
