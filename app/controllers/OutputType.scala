package controllers

import printing.PrinterOutputType

import scala.util.{Success, Try}

sealed trait OutputType {
  override def toString = this match {
    case OutputType.JSON => "json"
    case OutputType.Printer(printerOutputFormat) =>
      printerOutputFormat.toString
  }
}

object OutputType {
  case object JSON extends OutputType
  case class Printer(printerOutputType: PrinterOutputType) extends OutputType

  def apply(string: String): Try[OutputType] =
    string.toLowerCase match {
      case "json" => Success(JSON)
      case other  => PrinterOutputType(other).map(Printer.apply)
    }
}
