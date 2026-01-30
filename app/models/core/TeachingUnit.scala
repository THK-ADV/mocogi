package models.core

import java.util.UUID

case class TeachingUnit(id: UUID, label: String, abbrev: String, faculty: String) {
  def isINF = abbrev == "INF"
  def isING = abbrev == "ING"
}
