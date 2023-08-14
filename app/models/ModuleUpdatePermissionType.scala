package models

sealed trait ModuleUpdatePermissionType {
  def value: String
  override def toString = value
}

object ModuleUpdatePermissionType {
  case object Inherited extends ModuleUpdatePermissionType {
    override val value: String = "inherited"
  }
  case object Granted extends ModuleUpdatePermissionType {
    override val value: String = "granted"
  }

  def apply(value: String): ModuleUpdatePermissionType =
    value.toLowerCase match {
      case "inherited" => Inherited
      case "granted"   => Granted
    }
}
