package parsing.metadata

import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import parsing.ParserSpecHelper
import parsing.metadata.ResponsibilitiesParser.responsibilitiesParser

class ResponsibilitiesParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues {

  "A Responsibilities Parser" should {
    "return one coordinator and one lecturer" in {
      val resp =
        """responsibilities:
          |coordinator:person.abe
          |lecturers:person.ald
          |""".stripMargin
      val (res, rest) = responsibilitiesParser.run(resp)
      assert(res.value.coordinators.map(_.abbrev) == List("abe"))
      assert(res.value.lecturers.map(_.abbrev) == List("ald"))
      assert(rest.isEmpty)
    }

    "return one coordinator and one lecturer ignoring random whitespaces" in {
      val resp =
        """responsibilities:
          | coordinator: person.abe
          | lecturers: person.ald
          |""".stripMargin
      val (res, rest) = responsibilitiesParser.run(resp)
      assert(res.value.coordinators.map(_.abbrev) == List("abe"))
      assert(res.value.lecturers.map(_.abbrev) == List("ald"))
      assert(rest.isEmpty)
    }

    "return 2 coordinator which are seperated by dashes and 1 lecturer" in {
      val resp =
        """responsibilities:
          |coordinator:
          |-person.abe
          |-person.ddu
          |lecturers:person.ald
          |""".stripMargin
      val (res, rest) = responsibilitiesParser.run(resp)
      assert(res.value.coordinators.map(_.abbrev) == List("abe", "ddu"))
      assert(res.value.lecturers.map(_.abbrev) == List("ald"))
      assert(rest.isEmpty)
    }

    "return 2 coordinator which are seperated by dashes and 1 lecturer ignoring random whitespace" in {
      val resp =
        """responsibilities:
          | coordinator:
          | - person.abe
          | -person.ddu
          | lecturers: person.ald
          |""".stripMargin
      val (res, rest) = responsibilitiesParser.run(resp)
      assert(res.value.coordinators.map(_.abbrev) == List("abe", "ddu"))
      assert(res.value.lecturers.map(_.abbrev) == List("ald"))
      assert(rest.isEmpty)
    }

    "return 1 coordinator and 2 lecturer which are seperated by dashes" in {
      val resp =
        """responsibilities:
          |coordinator:person.abe
          |lecturers:
          |-person.ald
          |-person.ddu
          |""".stripMargin
      val (res, rest) = responsibilitiesParser.run(resp)
      assert(res.value.coordinators.map(_.abbrev) == List("abe"))
      assert(res.value.lecturers.map(_.abbrev) == List("ald", "ddu"))
      assert(rest.isEmpty)
    }

    "return 1 coordinator and 2 lecturer which are seperated by dashes ignoring whitespace" in {
      val resp =
        """responsibilities:
          | coordinator: person.abe
          | lecturers:
          | - person.ald
          | - person.ddu
          |""".stripMargin
      val (res, rest) = responsibilitiesParser.run(resp)
      assert(res.value.coordinators.map(_.abbrev) == List("abe"))
      assert(res.value.lecturers.map(_.abbrev) == List("ald", "ddu"))
      assert(rest.isEmpty)
    }

    "return 2 coordinator and 2 lecturer which are both seperated by dashes" in {
      val resp =
        """responsibilities:
          |coordinator:
          |  - person.abe
          |  - person.ald
          |lecturers:
          |  - person.ald
          |  - person.ddu
          |""".stripMargin
      val (res, rest) = responsibilitiesParser.run(resp)
      assert(res.value.coordinators.map(_.abbrev) == List("abe", "ald"))
      assert(res.value.lecturers.map(_.abbrev) == List("ald", "ddu"))
      assert(rest.isEmpty)
    }

    "return 2 coordinator and 2 lecturer which are both seperated by dashes ignoring random whitespace" in {
      val resp =
        """responsibilities:
          | coordinator:
          |  - person.abe
          |  - person.ald
          | lecturers:
          |  - person.ald
          |  - person.ddu
          |""".stripMargin
      val (res, rest) = responsibilitiesParser.run(resp)
      assert(res.value.coordinators.map(_.abbrev) == List("abe", "ald"))
      assert(res.value.lecturers.map(_.abbrev) == List("ald", "ddu"))
      assert(rest.isEmpty)
    }
  }
}
