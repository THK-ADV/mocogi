package models

sealed trait UniversityRole {
  def label: String
  def id: String
  override def toString = label
}

object UniversityRole {
  case object SGL extends UniversityRole {
    override val label: String = "Studiengangsleitung"
    override val id: String = "sgl"
  }
  case object PAV extends UniversityRole {
    override val label: String = "Prüfungsausschussvorsitzend"
    override val id: String = "pav"
  }

  def apply(id: String): UniversityRole =
    id.toLowerCase match {
      case "sgl" => SGL
      case "pav" => PAV
    }
}
