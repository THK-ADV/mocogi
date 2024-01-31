package models.core

import models.SpecializationShort
import play.api.libs.json.{Json, Writes}

case class Specialization(id: String, label: String, po: String)

object Specialization {
  implicit final class Ops(private val self: Specialization) extends AnyVal {
    def toShort: SpecializationShort =
      SpecializationShort(self.id, self.label)
  }

  implicit def writes: Writes[Specialization] = Json.writes
}
