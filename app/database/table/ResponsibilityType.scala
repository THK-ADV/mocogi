package database.table

sealed trait ResponsibilityType {
  override def toString = this match {
    case ResponsibilityType.Coordinator => "coordinator"
    case ResponsibilityType.Lecturer    => "lecturer"
  }
}

object ResponsibilityType {
  case object Coordinator extends ResponsibilityType
  case object Lecturer extends ResponsibilityType

  def apply(string: String): ResponsibilityType = string match {
    case "lecturer"    => Lecturer
    case "coordinator" => Coordinator
  }
}
