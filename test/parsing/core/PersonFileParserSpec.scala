package parsing.core

import helper.{FakeApplication, FakeFaculties}
import models.core.{Person, PersonStatus}
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.{ParserSpecHelper, withFile0}

final class PersonFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication
    with FakeFaculties {

  val personParser = app.injector.instanceOf(classOf[PersonFileParser])

  "A Person File Parser" should {
    "parse a unknown person" in {
      val input =
        """nn:
          |  title: N.N.""".stripMargin
      val (res1, rest1) = personParser.unknownParser.parse(input)
      assert(res1.value == Person.Unknown("nn", "N.N."))
      assert(rest1.isEmpty)
    }

    "parse a group" in {
      val input =
        """all:
          |  title: alle aktiven Lehrenden der Hochschule""".stripMargin
      val (res1, rest1) = personParser.parser.parse(input)
      assert(
        res1.value == List(
          Person.Group("all", "alle aktiven Lehrenden der Hochschule")
        )
      )
      assert(rest1.isEmpty)
    }

    "parse multiple groups" in {
      val input =
        """all:
          |  title: alle aktiven Lehrenden der Hochschule
          |all-f10:
          |  title: alle Lehrenden der F10
          |all-f10-prof:
          |  title: alle Professor:innen der F10
          |all-inf:
          |  title: alle Lehrenden der Lehreinheit Informatik
          |all-inf-prof:
          |  title: alle Professor:innen der Lehreinheit Informatik
          |all-ing:
          |  title: alle Lehrenden der Lehreinheit Ingenieurswesen
          |all-ing-prof:
          |  title: alle Professor:innen der Lehreinheit Ingenieurswesen""".stripMargin
      val (res1, rest1) = personParser.parser.parse(input)
      assert(
        res1.value == List(
          Person.Group("all", "alle aktiven Lehrenden der Hochschule"),
          Person.Group("all-f10", "alle Lehrenden der F10"),
          Person.Group("all-f10-prof", "alle Professor:innen der F10"),
          Person.Group("all-inf", "alle Lehrenden der Lehreinheit Informatik"),
          Person.Group(
            "all-inf-prof",
            "alle Professor:innen der Lehreinheit Informatik"
          ),
          Person
            .Group("all-ing", "alle Lehrenden der Lehreinheit Ingenieurswesen"),
          Person.Group(
            "all-ing-prof",
            "alle Professor:innen der Lehreinheit Ingenieurswesen"
          )
        )
      )
      assert(rest1.isEmpty)
    }

    "parse person status" in {
      val input1 = "status: active"
      val (res1, rest1) = personParser.statusParser.parse(input1)
      assert(res1.value == PersonStatus.Active)
      assert(rest1.isEmpty)

      val input2 = "status: inactive"
      val (res2, rest2) = personParser.statusParser.parse(input2)
      assert(res2.value == PersonStatus.Inactive)
      assert(rest2.isEmpty)

      val input3 = "status: other"
      val (res3, rest3) = personParser.statusParser.parse(input3)
      assert(res3.value == PersonStatus.Unknown)
      assert(rest3.isEmpty)
    }

    "parse a default person" in {
      val input =
        """abc:
          |  lastname: foo
          |  firstname: bar
          |  title: bar. baz.
          |  faculty:
          |    - faculty.f10
          |    - faculty.f03
          |  abbreviation: ab
          |  campusid: abc
          |  status: active""".stripMargin
      val (res1, rest1) = personParser.parser.parse(input)
      assert(
        res1.value == List(
          Person.Default(
            "abc",
            "foo",
            "bar",
            "bar. baz.",
            List(f10, f03),
            "ab",
            "abc",
            PersonStatus.Active
          )
        )
      )
      assert(rest1.isEmpty)
    }

    "parse multiple persons" in {
      val input =
        """abc:
          |  lastname: foo
          |  firstname: bar
          |  title: bar. baz.
          |  faculty:
          |    - faculty.f10
          |    - faculty.f03
          |  abbreviation: ab
          |  campusid: abc
          |  status: active
          |def:
          |  lastname: foo
          |  firstname: bar
          |  title: bar. baz.
          |  faculty: faculty.f10
          |  abbreviation: ab
          |  campusid: def
          |  status: inactive""".stripMargin
      val (res1, rest1) = personParser.parser.parse(input)
      assert(
        res1.value == List(
          Person.Default(
            "abc",
            "foo",
            "bar",
            "bar. baz.",
            List(f10, f03),
            "ab",
            "abc",
            PersonStatus.Active
          ),
          Person.Default(
            "def",
            "foo",
            "bar",
            "bar. baz.",
            List(f10),
            "ab",
            "def",
            PersonStatus.Inactive
          )
        )
      )
      assert(rest1.isEmpty)
    }

    "parse mixed persons" in {
      val input =
        """nn:
          |  title: N.N.
          |
          |all:
          |  title: alle aktiven Lehrenden der Hochschule
          |all-f10:
          |  title: alle Lehrenden der F10
          |
          |abc:
          |  lastname: foo
          |  firstname: bar
          |  title: bar. baz.
          |  faculty:
          |    - faculty.f10
          |    - faculty.f03
          |  abbreviation: ab
          |  campusid: abc
          |  status: active
          |def:
          |  lastname: foo
          |  firstname: bar
          |  title: bar. baz.
          |  faculty: faculty.f10
          |  abbreviation: ab
          |  campusid: def
          |  status: inactive""".stripMargin
      val (res1, rest1) = personParser.parser.parse(input)
      assert(
        res1.value == List(
          Person.Unknown("nn", "N.N."),
          Person.Group("all", "alle aktiven Lehrenden der Hochschule"),
          Person.Group("all-f10", "alle Lehrenden der F10"),
          Person.Default(
            "abc",
            "foo",
            "bar",
            "bar. baz.",
            List(f10, f03),
            "ab",
            "abc",
            PersonStatus.Active
          ),
          Person.Default(
            "def",
            "foo",
            "bar",
            "bar. baz.",
            List(f10),
            "ab",
            "def",
            PersonStatus.Inactive
          )
        )
      )
      assert(rest1.isEmpty)
    }

    "parse all people in person.yaml" in {
      val (res1, rest1) =
        withFile0("test/parsing/res/person.yaml")(personParser.parser.parse)
      assert(res1.value.size == 12)
      assert(res1.value.count(_.kind == Person.UnknownKind) == 1)
      assert(res1.value.count(_.kind == Person.GroupKind) == 7)
      assert(res1.value.count(_.kind == Person.DefaultKind) == 4)
      assert(rest1.isEmpty)
    }
  }
}
