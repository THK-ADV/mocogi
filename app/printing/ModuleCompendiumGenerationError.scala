package printing

import parser.ParsingError
import printer.PrintingError

sealed trait ModuleCompendiumGenerationError extends Throwable {
  override def getMessage = this match {
    case ModuleCompendiumGenerationError.Parsing(e)  => e.getMessage
    case ModuleCompendiumGenerationError.Printing(e) => e.getMessage
    case ModuleCompendiumGenerationError.Other(e)    => e.getMessage
  }
}

object ModuleCompendiumGenerationError {
  case class Parsing(e: ParsingError) extends ModuleCompendiumGenerationError
  case class Printing(e: PrintingError) extends ModuleCompendiumGenerationError
  case class Other(e: Throwable) extends ModuleCompendiumGenerationError
}
