package models

import play.api.libs.json.Writes

sealed trait ModuleUpdatePermissionType {
  def id: String
  override def toString = id
}

object ModuleUpdatePermissionType {
  case object Inherited extends ModuleUpdatePermissionType {
    override val id: String = "inherited"
  }
  case object Granted extends ModuleUpdatePermissionType {
    override val id: String = "granted"
  }

  def apply(id: String): ModuleUpdatePermissionType =
    id.toLowerCase match {
      case "inherited" => Inherited
      case "granted"   => Granted
    }

  implicit def writes: Writes[ModuleUpdatePermissionType] =
    Writes.of[String].contramap(_.id)
}
