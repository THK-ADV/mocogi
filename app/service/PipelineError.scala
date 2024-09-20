package service

import java.util.UUID

import parser.ParsingError
import play.api.libs.json.Json
import play.api.libs.json.Writes
import printer.PrintingError
import validation.ValidationError

sealed trait PipelineError extends Throwable {
  def metadata: Option[UUID]

  override def getMessage = this match {
    case PipelineError.Parser(error, id) =>
      s"id: $id\nmessage:${error.getMessage}"
    case PipelineError.Printer(error, id) =>
      s"id: $id\nmessage:${error.getMessage}"
    case PipelineError.Validator(error, id) =>
      s"id: $id\nmessage:${error.getMessage}"
  }
}

object PipelineError {
  case class Parser(error: ParsingError, metadata: Option[UUID])       extends PipelineError
  case class Printer(error: PrintingError, metadata: Option[UUID])     extends PipelineError
  case class Validator(error: ValidationError, metadata: Option[UUID]) extends PipelineError

  implicit def parsingErrorWrites: Writes[ParsingError] = Json.writes

  implicit def printingErrorWrites: Writes[PrintingError] = Json.writes

  implicit def validationErrorWrites: Writes[ValidationError] = Json.writes

  private val ParsingErrorTag = "parsing-error"

  private val PrintingErrorTag = "printing-error"

  private val ValidationErrorTag = "validation-error"

  implicit def writes: Writes[PipelineError] = {
    case PipelineError.Parser(e, id) =>
      Json.obj(
        "tag"      -> ParsingErrorTag,
        "metadata" -> id,
        "error"    -> parsingErrorWrites.writes(e)
      )
    case PipelineError.Printer(e, id) =>
      Json.obj(
        "tag"      -> PrintingErrorTag,
        "metadata" -> id,
        "error"    -> printingErrorWrites.writes(e)
      )
    case PipelineError.Validator(e, id) =>
      Json.obj(
        "tag"      -> ValidationErrorTag,
        "metadata" -> id,
        "error"    -> validationErrorWrites.writes(e)
      )
  }
}
