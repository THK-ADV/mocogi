package parsing.core

import helper.FakeApplication
import models.core.ModuleStatus
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.{ParserSpecHelper, withFile0}

final class ModuleStatusFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication {

  val parser = StatusFileParser.fileParser

  "A Status Filer Parser" should {
    "parse a single status" in {
      val input =
        """active:
          |  de_label: Aktiv
          |  en_label: active""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(ModuleStatus("active", "Aktiv", "active"))
      )
      assert(rest.isEmpty)
    }

    "parse multiple status" in {
      val input =
        """active:
          |  de_label: Aktiv
          |  en_label: active
          |
          |inactive:
          |  de_label: Inaktiv
          |  en_label: inactive""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(
          ModuleStatus("active", "Aktiv", "active"),
          ModuleStatus("inactive", "Inaktiv", "inactive")
        )
      )
      assert(rest.isEmpty)
    }

    "parse all status in status.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/status.yaml")(parser.parse)
      assert(
        res.value == List(
          ModuleStatus("active", "Aktiv", "--"),
          ModuleStatus("inactive", "Inaktiv", "--")
        )
      )
      assert(rest.isEmpty)
    }
  }
}
