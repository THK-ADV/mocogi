package printing

import parser.ParsingError
import printer.PrintingError

sealed trait ModuleCompendiumGenerationError

object ModuleCompendiumGenerationError {
  case class Parsing(e: ParsingError) extends ModuleCompendiumGenerationError
  case class Printing(e: PrintingError) extends ModuleCompendiumGenerationError
  case class Other(e: Throwable) extends ModuleCompendiumGenerationError
}
