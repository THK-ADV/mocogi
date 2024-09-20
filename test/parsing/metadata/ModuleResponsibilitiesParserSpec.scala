package parsing.metadata

import cats.data.NonEmptyList
import helper.FakeIdentities
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import parsing.ParserSpecHelper

class ModuleResponsibilitiesParserSpec extends AnyWordSpec with ParserSpecHelper with EitherValues with FakeIdentities {

  val parser = ModuleResponsibilitiesParser.parser
  val raw    = ModuleResponsibilitiesParser.raw

  "A Responsibilities Parser" should {
    "return one coordinator and one lecturer" in {
      val resp =
        """responsibilities:
          |module_management:person.abe
          |lecturers:person.ald
          |""".stripMargin
      val (res, rest) = parser.parse(resp)
      assert(res.value.moduleManagement.map(_.id) == NonEmptyList.one("abe"))
      assert(res.value.lecturers.map(_.id) == NonEmptyList.one("ald"))
      assert(rest.isEmpty)
    }

    "return one coordinator and one lecturer raw" in {
      val resp =
        """responsibilities:
          |module_management:person.abe
          |lecturers:person.ald
          |""".stripMargin
      val (res, rest) = raw.parse(resp)
      assert(res.value._1 == NonEmptyList.one("abe"))
      assert(res.value._2 == NonEmptyList.one("ald"))
      assert(rest.isEmpty)
    }

    "return one coordinator and one lecturer ignoring random whitespaces" in {
      val resp =
        """responsibilities:
          | module_management: person.abe
          | lecturers: person.ald
          |""".stripMargin
      val (res, rest) = parser.parse(resp)
      assert(res.value.moduleManagement.map(_.id) == NonEmptyList.one("abe"))
      assert(res.value.lecturers.map(_.id) == NonEmptyList.one("ald"))
      assert(rest.isEmpty)
    }

    "return 2 coordinator which are seperated by dashes and 1 lecturer" in {
      val resp =
        """responsibilities:
          |module_management:
          |-person.abe
          |-person.ddu
          |lecturers:person.ald
          |""".stripMargin
      val (res, rest) = parser.parse(resp)
      assert(
        res.value.moduleManagement.map(_.id) == NonEmptyList.of("abe", "ddu")
      )
      assert(res.value.lecturers.map(_.id) == NonEmptyList.one("ald"))
      assert(rest.isEmpty)
    }

    "return 2 coordinator which are seperated by dashes and 1 lecturer raw" in {
      val resp =
        """responsibilities:
          |module_management:
          |-person.abe
          |-person.ddu
          |lecturers:person.ald
          |""".stripMargin
      val (res, rest) = raw.parse(resp)
      assert(res.value._1 == NonEmptyList.of("abe", "ddu"))
      assert(res.value._2 == NonEmptyList.one("ald"))
      assert(rest.isEmpty)
    }

    "return 2 coordinator which are seperated by dashes and 1 lecturer ignoring random whitespace" in {
      val resp =
        """responsibilities:
          | module_management:
          | - person.abe
          | -person.ddu
          | lecturers: person.ald
          |""".stripMargin
      val (res, rest) = parser.parse(resp)
      assert(
        res.value.moduleManagement.map(_.id) == NonEmptyList.of("abe", "ddu")
      )
      assert(res.value.lecturers.map(_.id) == NonEmptyList.one("ald"))
      assert(rest.isEmpty)
    }

    "return 1 coordinator and 2 lecturer which are seperated by dashes" in {
      val resp =
        """responsibilities:
          |module_management:person.abe
          |lecturers:
          |-person.ald
          |-person.ddu
          |""".stripMargin
      val (res, rest) = parser.parse(resp)
      assert(res.value.moduleManagement.map(_.id) == NonEmptyList.one("abe"))
      assert(res.value.lecturers.map(_.id) == NonEmptyList.of("ald", "ddu"))
      assert(rest.isEmpty)
    }

    "return 1 coordinator and 2 lecturer which are seperated by dashes raw" in {
      val resp =
        """responsibilities:
          |module_management:person.abe
          |lecturers:
          |-person.ald
          |-person.ddu
          |""".stripMargin
      val (res, rest) = raw.parse(resp)
      assert(res.value._1 == NonEmptyList.one("abe"))
      assert(res.value._2 == NonEmptyList.of("ald", "ddu"))
      assert(rest.isEmpty)
    }

    "return 1 coordinator and 2 lecturer which are seperated by dashes ignoring whitespace" in {
      val resp =
        """responsibilities:
          | module_management: person.abe
          | lecturers:
          | - person.ald
          | - person.ddu
          |""".stripMargin
      val (res, rest) = parser.parse(resp)
      assert(res.value.moduleManagement.map(_.id) == NonEmptyList.one("abe"))
      assert(res.value.lecturers.map(_.id) == NonEmptyList.of("ald", "ddu"))
      assert(rest.isEmpty)
    }

    "return 2 coordinator and 2 lecturer which are both seperated by dashes" in {
      val resp =
        """responsibilities:
          |module_management:
          |  - person.abe
          |  - person.ald
          |lecturers:
          |  - person.ald
          |  - person.ddu
          |""".stripMargin
      val (res, rest) = parser.parse(resp)
      assert(
        res.value.moduleManagement.map(_.id) == NonEmptyList.of("abe", "ald")
      )
      assert(res.value.lecturers.map(_.id) == NonEmptyList.of("ald", "ddu"))
      assert(rest.isEmpty)
    }

    "return 2 coordinator and 2 lecturer which are both seperated by dashes raw" in {
      val resp =
        """responsibilities:
          |module_management:
          |  - person.abe
          |  - person.ald
          |lecturers:
          |  - person.ald
          |  - person.ddu
          |""".stripMargin
      val (res, rest) = raw.parse(resp)
      assert(res.value._1 == NonEmptyList.of("abe", "ald"))
      assert(res.value._2 == NonEmptyList.of("ald", "ddu"))
      assert(rest.isEmpty)
    }

    "return 2 coordinator and 2 lecturer which are both seperated by dashes ignoring random whitespace" in {
      val resp =
        """responsibilities:
          | module_management:
          |  - person.abe
          |  - person.ald
          | lecturers:
          |  - person.ald
          |  - person.ddu
          |""".stripMargin
      val (res, rest) = parser.parse(resp)
      assert(
        res.value.moduleManagement.map(_.id) == NonEmptyList.of("abe", "ald")
      )
      assert(res.value.lecturers.map(_.id) == NonEmptyList.of("ald", "ddu"))
      assert(rest.isEmpty)
    }
  }
}
