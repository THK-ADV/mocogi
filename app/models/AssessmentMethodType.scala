package models

sealed trait AssessmentMethodType {
  def id: String
  override def toString = id
}

object AssessmentMethodType {
  case object Mandatory extends AssessmentMethodType {
    override val id: String = "mandatory"
  }
  case object Optional extends AssessmentMethodType {
    override val id: String = "optional"
  }

  def apply(id: String): AssessmentMethodType =
    id.toLowerCase match {
      case "mandatory" => Mandatory
      case "optional"  => Optional
    }
}
