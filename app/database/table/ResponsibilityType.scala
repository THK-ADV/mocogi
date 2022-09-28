package database.table

sealed trait ResponsibilityType {
  override def toString = this match {
    case ResponsibilityType.ModuleManagement => "module_management"
    case ResponsibilityType.Lecturer    => "lecturer"
  }
}

object ResponsibilityType {
  case object ModuleManagement extends ResponsibilityType
  case object Lecturer extends ResponsibilityType

  def apply(string: String): ResponsibilityType = string match {
    case "lecturer"    => Lecturer
    case "module_management" => ModuleManagement
  }
}
