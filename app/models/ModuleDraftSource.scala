package models

import play.api.libs.json.Format

sealed trait ModuleDraftSource {
  def id: String
  def isAdded: Boolean
  override def toString = id
}

object ModuleDraftSource {

  implicit val fmt: Format[ModuleDraftSource] =
    Format.of[String].bimap(apply, _.id)

  case object Added extends ModuleDraftSource {
    override val id: String = "added"
    override def isAdded: Boolean = true
  }

  case object Modified extends ModuleDraftSource {
    override val id: String = "modified"
    override def isAdded: Boolean = false
  }

  def apply(id: String): ModuleDraftSource =
    id.toLowerCase match {
      case "added"    => Added
      case "modified" => Modified
    }
}
