package parsing

import java.util.UUID

import io.circe.ParsingFailure
import parser.ParsingError

case class YamlParsingError(module: UUID, err: ParsingFailure | ParsingError) extends Exception {
  override def getMessage = {
    val errMsg = err match
      case pf: ParsingFailure => pf.toString()
      case pe: ParsingError   => pe.getMessage
    s"failed to parse module $module. error:\n$errMsg"
  }

  override def toString = s"YamlParsingError: $getMessage"
}
