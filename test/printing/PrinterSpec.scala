package printing

import org.scalatest.EitherValues
import printer.Printer

trait PrinterSpec extends EitherValues {
  def run(printer: Printer[Unit]): String =
    printer.print((), new StringBuilder()).value.toString()
}
