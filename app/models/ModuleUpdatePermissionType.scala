package models

import models.core.IDLabel
import play.api.libs.json.Writes

sealed trait ModuleUpdatePermissionType extends IDLabel {
  def id: String
  def deLabel: String
  def enLabel: String
  def isInherited       = this == ModuleUpdatePermissionType.Inherited
  override def toString = id
}

object ModuleUpdatePermissionType {
  case object Inherited extends ModuleUpdatePermissionType {
    override def id: String      = "inherited"
    override def deLabel: String = "Automatisch gesetzt"
    override def enLabel: String = "Inherited"
  }

  case object Granted extends ModuleUpdatePermissionType {
    override def id: String      = "granted"
    override def deLabel: String = "Explizit vergeben"
    override def enLabel: String = "Granted"
  }

  def apply(id: String): ModuleUpdatePermissionType =
    id.toLowerCase match {
      case "inherited" => Inherited
      case "granted"   => Granted
    }

  implicit def writes: Writes[ModuleUpdatePermissionType] =
    Writes.of[IDLabel].contramap(identity)
}
