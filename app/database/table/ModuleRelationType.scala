package database.table

sealed trait ModuleRelationType {
  override def toString = this match {
    case ModuleRelationType.Parent => "parent"
    case ModuleRelationType.Child  => "child"
  }
}

object ModuleRelationType {
  case object Parent extends ModuleRelationType
  case object Child extends ModuleRelationType

  def apply(string: String): ModuleRelationType =
    string.toLowerCase match {
      case "parent" => Parent
      case "child"  => Child
    }
}
