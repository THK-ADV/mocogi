package models

import cats.data.NonEmptyList
import controllers.NelWrites
import play.api.libs.json._

import java.util.UUID

sealed trait ModuleRelationProtocol

object ModuleRelationProtocol extends NelWrites {
  case class Parent(children: NonEmptyList[UUID]) extends ModuleRelationProtocol
  case class Child(parent: UUID) extends ModuleRelationProtocol

  implicit def format: Format[ModuleRelationProtocol] =
    OFormat.apply(
      js =>
        js.\("kind")
          .validate[String]
          .flatMap {
            case "parent" =>
              js.\("children")
                .validate[List[UUID]]
                .flatMap(xs =>
                  NonEmptyList
                    .fromList(xs)
                    .fold[JsResult[ModuleRelationProtocol]](
                      JsError("expected at least one child")
                    )(children =>
                      JsSuccess(ModuleRelationProtocol.Parent(children))
                    )
                )
            case "child" =>
              js.\("parent")
                .validate[UUID]
                .map(ModuleRelationProtocol.Child.apply)
            case other =>
              JsError(s"expected kind to be parent or child, but was $other")
          },
      (p: ModuleRelationProtocol) =>
        p match {
          case ModuleRelationProtocol.Parent(children) =>
            Json.obj(
              "kind" -> "parent",
              "children" -> Json.toJson(children)
            )
          case ModuleRelationProtocol.Child(parent) =>
            Json.obj(
              "kind" -> "child",
              "parent" -> Json.toJson(parent)
            )
        }
    )
}
