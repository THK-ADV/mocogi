package parsing.metadata

import helper.{FakeApplication, FakeIdentities}
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.ParserSpecHelper

class ModuleResponsibilitiesParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication
    with FakeIdentities {

  val parser = app.injector.instanceOf(classOf[ModuleResponsibilitiesParser]).parser

  "A Responsibilities Parser" should {
    "return one coordinator and one lecturer" in {
      val resp =
        """responsibilities:
          |module_management:person.abe
          |lecturers:person.ald
          |""".stripMargin
      val (res, rest) = parser.parse(resp)
      assert(res.value.moduleManagement.map(_.id) == List("abe"))
      assert(res.value.lecturers.map(_.id) == List("ald"))
      assert(rest.isEmpty)
    }

    "return one coordinator and one lecturer ignoring random whitespaces" in {
      val resp =
        """responsibilities:
          | module_management: person.abe
          | lecturers: person.ald
          |""".stripMargin
      val (res, rest) = parser.parse(resp)
      assert(res.value.moduleManagement.map(_.id) == List("abe"))
      assert(res.value.lecturers.map(_.id) == List("ald"))
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
      assert(res.value.moduleManagement.map(_.id) == List("abe", "ddu"))
      assert(res.value.lecturers.map(_.id) == List("ald"))
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
      assert(res.value.moduleManagement.map(_.id) == List("abe", "ddu"))
      assert(res.value.lecturers.map(_.id) == List("ald"))
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
      assert(res.value.moduleManagement.map(_.id) == List("abe"))
      assert(res.value.lecturers.map(_.id) == List("ald", "ddu"))
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
      assert(res.value.moduleManagement.map(_.id) == List("abe"))
      assert(res.value.lecturers.map(_.id) == List("ald", "ddu"))
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
      assert(res.value.moduleManagement.map(_.id) == List("abe", "ald"))
      assert(res.value.lecturers.map(_.id) == List("ald", "ddu"))
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
      assert(res.value.moduleManagement.map(_.id) == List("abe", "ald"))
      assert(res.value.lecturers.map(_.id) == List("ald", "ddu"))
      assert(rest.isEmpty)
    }
  }
}
