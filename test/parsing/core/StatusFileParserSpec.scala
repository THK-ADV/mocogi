package parsing.core

import helper.FakeApplication
import models.core.Status
import org.scalatest.EitherValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.core.StatusFileParser
import parsing.{ParserSpecHelper, withFile0}

final class StatusFileParserSpec
    extends AnyWordSpec
    with ParserSpecHelper
    with EitherValues
    with GuiceOneAppPerSuite
    with FakeApplication {

  val parser = app.injector.instanceOf(classOf[StatusFileParser]).fileParser

  "A Status Filer Parser" should {
    "parse a single status" in {
      val input =
        """active:
          |  de_label: Aktiv
          |  en_label: active""".stripMargin
      val (res, rest) = parser.parse(input)
      assert(
        res.value == List(Status("active", "Aktiv", "active"))
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
          Status("active", "Aktiv", "active"),
          Status("inactive", "Inaktiv", "inactive")
        )
      )
      assert(rest.isEmpty)
    }

    "parse all status in status.yaml" in {
      val (res, rest) =
        withFile0("test/parsing/res/status.yaml")(parser.parse)
      assert(
        res.value == List(
          Status("active", "Aktiv", "--"),
          Status("inactive", "Inaktiv", "--")
        )
      )
      assert(rest.isEmpty)
    }
  }
}
