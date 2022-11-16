package database.table

sealed trait AssessmentMethodType {
  override def toString = this match {
    case AssessmentMethodType.Mandatory => "mandatory"
    case AssessmentMethodType.Optional  => "optional"
  }
}

object AssessmentMethodType {
  case object Mandatory extends AssessmentMethodType
  case object Optional extends AssessmentMethodType

  def apply(string: String): AssessmentMethodType =
    string.toLowerCase match {
      case "mandatory" => Mandatory
      case "optional"  => Optional
    }
}
