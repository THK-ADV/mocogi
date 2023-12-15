package models.core

import models.SpecializationShort

case class Specialization(abbrev: String, label: String, po: String)

object Specialization {
  implicit final class Ops(private val self: Specialization) extends AnyVal {
    def toShort: SpecializationShort =
      SpecializationShort(self.abbrev, self.label)
  }
}
