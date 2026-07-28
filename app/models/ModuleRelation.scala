package models

import cats.data.NonEmptyList
import controllers.json.NelWrites
import play.api.libs.json.Json
import play.api.libs.json.Writes

case class ModuleRelation(children: NonEmptyList[ModuleCore])

object ModuleRelation extends NelWrites {
  implicit def writes: Writes[ModuleRelation] = relation =>
    Json.obj(
      "kind"     -> "parent",
      "children" -> Json.toJson(relation.children)
    )
}
