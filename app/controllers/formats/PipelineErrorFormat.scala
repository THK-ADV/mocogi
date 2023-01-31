package controllers.formats

import parser.ParsingError
import play.api.libs.json.{Format, JsError, Json}
import printer.PrintingError
import service.PipelineError
import validator.ValidationError

import java.util.UUID

trait PipelineErrorFormat {
  implicit val parsingErrorFormat: Format[ParsingError] =
    Json.format[ParsingError]

  implicit val printingErrorFormat: Format[PrintingError] =
    Json.format[PrintingError]

  implicit val validationErrorFormat: Format[ValidationError] =
    Json.format[ValidationError]

  private val ParsingError = "parsing-error"

  private val PrintingError = "printing-error"

  private val ValidationError = "validation-error"

  implicit val pipelineErrorFormat: Format[PipelineError] =
    Format.apply(
      json =>
        for {
          metadata <- json.\("metadata").validate[UUID]
          tag <- json.\("tag").validate[String]
          errorJson = json.\("error")
          res <- tag match {
            case ParsingError =>
              errorJson
                .validate(parsingErrorFormat)
                .map(PipelineError.Parser(_, metadata))
            case PrintingError =>
              errorJson
                .validate(printingErrorFormat)
                .map(PipelineError.Printer(_, metadata))
            case ValidationError =>
              errorJson
                .validate(validationErrorFormat)
                .map(PipelineError.Validator(_, metadata))
            case other => JsError(s"invalid pipeline error tag $other")
          }
        } yield res,
      {
        case PipelineError.Parser(e, id) =>
          Json.obj(
            "tag" -> ParsingError,
            "metadata" -> id,
            "error" -> parsingErrorFormat.writes(e)
          )
        case PipelineError.Printer(e, id) =>
          Json.obj(
            "tag" -> PrintingError,
            "metadata" -> id,
            "error" -> printingErrorFormat.writes(e)
          )
        case PipelineError.Validator(e, id) =>
          Json.obj(
            "tag" -> ValidationError,
            "metadata" -> id,
            "error" -> validationErrorFormat.writes(e)
          )
      }
    )
}
