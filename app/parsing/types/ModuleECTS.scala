package parsing.types

import play.api.libs.json.Writes

case class ModuleECTS(value: Double) extends AnyVal

object ModuleECTS {
  implicit def writes: Writes[ModuleECTS] = Writes.DoubleWrites.contramap(_.value)
}
