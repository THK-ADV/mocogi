package models

import play.api.libs.json.{Json, Writes}

sealed trait ModuleUpdatePermissionType {
  def id: String
  def deLabel: String
  def enLabel: String
  override def toString = id
}

object ModuleUpdatePermissionType {
  case object Inherited extends ModuleUpdatePermissionType {
    override def id: String = "inherited"
    override def deLabel: String = "Automatisch gesetzt"
    override def enLabel: String = "Inherited"
  }

  case object Granted extends ModuleUpdatePermissionType {
    override def id: String = "granted"
    override def deLabel: String = "Explizit vergeben"
    override def enLabel: String = "Granted"
  }

  def apply(id: String): ModuleUpdatePermissionType =
    id.toLowerCase match {
      case "inherited" => Inherited
      case "granted"   => Granted
    }

  implicit def writes: Writes[ModuleUpdatePermissionType] =
    o =>
      Json.obj(
        "id" -> o.id,
        "deLabel" -> o.deLabel,
        "enLabel" -> o.enLabel
      )
}
