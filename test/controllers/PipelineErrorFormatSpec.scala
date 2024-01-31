package controllers

import org.scalatest.OptionValues
import org.scalatest.wordspec.AnyWordSpec
import parser.ParsingError
import play.api.libs.json.{JsArray, JsString}
import printer.PrintingError
import service.PipelineError
import validator.ValidationError

import java.util.UUID

final class PipelineErrorFormatSpec extends AnyWordSpec with OptionValues {

  "A PipelineErrorFormat Spec" should {
    "serialize a parsing error" in {
      val medata = UUID.randomUUID
      val parsingError = ParsingError("exp", "fo")
      val res =
        PipelineError.writes.writes(
          PipelineError.Parser(parsingError, Some(medata))
        )
      assert(res.\("tag").get == JsString("parsing-error"))
      assert(res.\("metadata").get == JsString(medata.toString))
      assert(res.\("error").\("expected").get == JsString("exp"))
      assert(res.\("error").\("found").get == JsString("fo"))
    }

    "serialize a printing error" in {
      val medata = UUID.randomUUID
      val printingError = PrintingError("exp", "fo")
      val res =
        PipelineError.writes.writes(
          PipelineError.Printer(printingError, Some(medata))
        )
      assert(res.\("tag").get == JsString("printing-error"))
      assert(res.\("metadata").get == JsString(medata.toString))
      assert(res.\("error").\("expected").get == JsString("exp"))
      assert(res.\("error").\("found").get == JsString("fo"))
    }

    "serialize a validation error" in {
      val metadata = UUID.randomUUID()
      val validationError = ValidationError(List("abc", "def"))
      val res =
        PipelineError.writes.writes(
          PipelineError.Validator(validationError, Some(metadata))
        )
      assert(res.\("tag").get == JsString("validation-error"))
      assert(res.\("metadata").get == JsString(metadata.toString))
      assert(
        res.\("error").\("errs").get == JsArray(
          Seq(JsString("abc"), JsString("def"))
        )
      )
    }
  }
}
