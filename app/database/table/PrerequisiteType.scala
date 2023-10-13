package database.table

sealed trait PrerequisiteType {
  def id: String
  override def toString = id
}

object PrerequisiteType {
  case object Required extends PrerequisiteType {
    override val id: String = "required"
  }
  case object Recommended extends PrerequisiteType {
    override val id: String = "recommended"
  }

  def apply(id: String): PrerequisiteType =
    id match {
      case "required"    => Required
      case "recommended" => Recommended
    }
}
