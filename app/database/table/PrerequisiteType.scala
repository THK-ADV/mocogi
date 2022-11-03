package database.table

sealed trait PrerequisiteType {
  override def toString = this match {
    case PrerequisiteType.Required    => "required"
    case PrerequisiteType.Recommended => "recommended"
  }
}

object PrerequisiteType {
  case object Required extends PrerequisiteType
  case object Recommended extends PrerequisiteType

  def apply(string: String): PrerequisiteType = string match {
    case "required"    => Required
    case "recommended" => Recommended
  }
}
