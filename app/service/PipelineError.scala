package service

import parser.ParsingError
import printer.PrintingError
import validator.ValidationError

import java.util.UUID

sealed trait PipelineError {
  def metadata: UUID
}

object PipelineError {
  case class Parser(error: ParsingError, metadata: UUID) extends PipelineError
  case class Printer(error: PrintingError, metadata: UUID) extends PipelineError
  case class Validator(error: ValidationError, metadata: UUID)
      extends PipelineError
}
