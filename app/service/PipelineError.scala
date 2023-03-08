package service

import parser.ParsingError
import printer.PrintingError
import validator.ValidationError

import java.util.UUID

sealed trait PipelineError extends Throwable {
  def metadata: Option[UUID]

  override def getMessage = this match {
    case PipelineError.Parser(error, _)    => error.getMessage()
    case PipelineError.Printer(error, _)   => error.getMessage()
    case PipelineError.Validator(error, _) => error.getMessage
  }
}

object PipelineError {
  case class Parser(error: ParsingError, metadata: Option[UUID])
      extends PipelineError
  case class Printer(error: PrintingError, metadata: Option[UUID])
      extends PipelineError
  case class Validator(error: ValidationError, metadata: Option[UUID])
      extends PipelineError
}
