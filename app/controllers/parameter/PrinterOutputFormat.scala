package controllers.parameter

import scala.util.{Success, Try}

sealed trait PrinterOutputFormat {
  override def toString = this match {
    case PrinterOutputFormat.DefaultPrinter => "default"
  }
}

object PrinterOutputFormat {
  case object DefaultPrinter extends PrinterOutputFormat

  def apply(string: String): Try[PrinterOutputFormat] = string.toLowerCase match {
    case "default" => Success(DefaultPrinter)
  }
}
