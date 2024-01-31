package models

sealed trait ModuleRelationType {
  def id: String
  override def toString = id
}

object ModuleRelationType {
  case object Parent extends ModuleRelationType {
    override val id: String = "parent"
  }
  case object Child extends ModuleRelationType {
    override val id: String = "child"
  }

  def apply(id: String): ModuleRelationType =
    id.toLowerCase match {
      case "parent" => Parent
      case "child"  => Child
    }
}
