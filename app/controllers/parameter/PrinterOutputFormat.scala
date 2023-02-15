package controllers.parameter

import printing.markdown.{ModuleCompendiumMarkdownPrinter, ModuleCompendiumPrinter}

import scala.util.{Success, Try}

sealed trait PrinterOutputFormat {
  def printer: ModuleCompendiumPrinter
}

object PrinterOutputFormat {
  case object DefaultPrinter extends PrinterOutputFormat {
    override def printer = ModuleCompendiumMarkdownPrinter
    override def toString = "default"
  }

  def apply(string: String): Try[PrinterOutputFormat] =
    string.toLowerCase match {
      case "default" => Success(DefaultPrinter)
    }
}
