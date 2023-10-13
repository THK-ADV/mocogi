package models

sealed trait ModuleReviewerRole {
  def label: String
  def id: String
  override def toString = label
}

object ModuleReviewerRole {
  case object SGL extends ModuleReviewerRole {
    override val label: String = "Studiengangsleitung"
    override val id: String = "sgl"
  }
  case object PAV extends ModuleReviewerRole {
    override val label: String = "Prüfungsausschussvorsitzend"
    override val id: String = "pav"
  }

  def apply(id: String): ModuleReviewerRole =
    id.toLowerCase match {
      case "sgl" => SGL
      case "pav" => PAV
    }
}
