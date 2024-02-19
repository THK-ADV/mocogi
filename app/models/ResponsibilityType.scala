package models

sealed trait ResponsibilityType {
  def id: String
  override def toString = id
}

object ResponsibilityType {
  case object ModuleManagement extends ResponsibilityType {
    override val id: String = "module_management"
  }
  case object Lecturer extends ResponsibilityType {
    override val id: String = "lecturer"
  }

  def apply(id: String): ResponsibilityType =
    id match {
      case "lecturer"          => Lecturer
      case "module_management" => ModuleManagement
    }
}
