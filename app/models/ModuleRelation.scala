package models

sealed trait ModuleRelation

import play.api.libs.json.{Json, Writes}

object ModuleRelation {
  case class Parent(children: List[ModuleCore]) extends ModuleRelation
  case class Child(parent: ModuleCore) extends ModuleRelation

  implicit def writes: Writes[ModuleRelation] = {
    case ModuleRelation.Parent(children) =>
      Json.obj(
        "kind" -> "parent",
        "children" -> Json.toJson(children)
      )
    case ModuleRelation.Child(parent) =>
      Json.obj(
        "kind" -> "child",
        "parent" -> Json.toJson(parent)
      )
  }
}
