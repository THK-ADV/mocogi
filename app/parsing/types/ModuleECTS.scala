package parsing.types

import play.api.libs.json.Writes

case class ModuleECTS(value: Double) extends AnyVal

object ModuleECTS {
  given Writes[ModuleECTS] = Writes.DoubleWrites.contramap(_.value)
}
