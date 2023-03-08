package controllers

import controllers.formats.PipelineErrorFormat
import org.scalatest.OptionValues
import org.scalatest.wordspec.AnyWordSpec
import parser.ParsingError
import play.api.libs.json.{JsArray, JsString, Json}
import printer.PrintingError
import service.PipelineError
import validator.ValidationError

import java.util.UUID

final class PipelineErrorFormatSpec
    extends AnyWordSpec
    with PipelineErrorFormat
    with OptionValues {

  "A PipelineErrorFormat Spec" should {
    "serialize back and forth a parsing error" in {
      val medata = Some(UUID.randomUUID)
      val parsingError = ParsingError("exp", "fo")
      val res =
        pipelineErrorFormat.writes(PipelineError.Parser(parsingError, medata))
      assert(res.\("tag").get == JsString("parsing-error"))
      assert(res.\("metadata").get == JsString(medata.toString))
      assert(res.\("error").\("expected").get == JsString("exp"))
      assert(res.\("error").\("found").get == JsString("fo"))

      val json = Json.obj(
        "tag" -> "parsing-error",
        "metadata" -> medata,
        "error" -> Json.obj("expected" -> "abc", "found" -> "def")
      )
      pipelineErrorFormat.reads(json).get match {
        case PipelineError.Parser(error, id) =>
          assert(id == medata)
          assert(error.expected == "abc")
          assert(error.found == "def")
        case PipelineError.Printer(error, _) =>
          fail(s"expected parsing error, but was $error")
        case PipelineError.Validator(error, _) =>
          fail(s"expected parsing error, but was $error")
      }
    }

    "serialize back and forth a printing error" in {
      val medata = Some(UUID.randomUUID)
      val printingError = PrintingError("exp", "fo")
      val res =
        pipelineErrorFormat.writes(PipelineError.Printer(printingError, medata))
      assert(res.\("tag").get == JsString("printing-error"))
      assert(res.\("metadata").get == JsString(medata.toString))
      assert(res.\("error").\("expected").get == JsString("exp"))
      assert(res.\("error").\("found").get == JsString("fo"))

      val json = Json.obj(
        "tag" -> "printing-error",
        "metadata" -> medata,
        "error" -> Json.obj("expected" -> "abc", "found" -> "def")
      )
      pipelineErrorFormat.reads(json).get match {
        case PipelineError.Parser(error, _) =>
          fail(s"expected printing error, but was $error")
        case PipelineError.Printer(error, id) =>
          assert(id == medata)
          assert(error.expected == "abc")
          assert(error.found == "def")
        case PipelineError.Validator(error, _) =>
          fail(s"expected printing error, but was $error")
      }
    }

    "serialize back and forth a validation error" in {
      val metadata = Some(UUID.randomUUID())
      val validationError = ValidationError(List("abc", "def"))
      val res =
        pipelineErrorFormat.writes(
          PipelineError.Validator(validationError, metadata)
        )
      assert(res.\("tag").get == JsString("validation-error"))
      assert(res.\("metadata").get == JsString(metadata.toString))
      assert(
        res.\("error").\("errs").get == JsArray(
          Seq(JsString("abc"), JsString("def"))
        )
      )

      val json = Json.obj(
        "tag" -> "validation-error",
        "metadata" -> metadata,
        "error" -> Json.obj(
          "errs" -> List("123", "456")
        )
      )
      pipelineErrorFormat.reads(json).get match {
        case PipelineError.Parser(error, _) =>
          fail(s"expected validation error, but was $error")
        case PipelineError.Printer(error, _) =>
          fail(s"expected validation error, but was $error")
        case PipelineError.Validator(error, id) =>
          assert(id == metadata)
          assert(error.errs == List("123", "456"))
      }
    }
  }
}
