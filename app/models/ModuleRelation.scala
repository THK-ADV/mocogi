package models

sealed trait ModuleRelation

import cats.data.NonEmptyList
import controllers.NelWrites
import play.api.libs.json.Json
import play.api.libs.json.Writes

object ModuleRelation extends NelWrites {
  case class Parent(children: NonEmptyList[ModuleCore]) extends ModuleRelation
  case class Child(parent: ModuleCore)                  extends ModuleRelation

  implicit def writes: Writes[ModuleRelation] = {
    case ModuleRelation.Parent(children) =>
      Json.obj(
        "kind"     -> "parent",
        "children" -> Json.toJson(children)
      )
    case ModuleRelation.Child(parent) =>
      Json.obj(
        "kind"   -> "child",
        "parent" -> Json.toJson(parent)
      )
  }
}
