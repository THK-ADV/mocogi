package validator

sealed trait ModuleRelation

import models.Module
import play.api.libs.json.{Json, Writes}

object ModuleRelation {
  case class Parent(children: List[Module]) extends ModuleRelation
  case class Child(parent: Module) extends ModuleRelation

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
