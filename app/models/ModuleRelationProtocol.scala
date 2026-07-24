package models

import java.util.UUID

import cats.data.NonEmptyList
import controllers.json.NelWrites
import play.api.libs.json.*

case class ModuleRelationProtocol(children: NonEmptyList[UUID])

object ModuleRelationProtocol extends NelWrites {
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
                    )(children => JsSuccess(ModuleRelationProtocol(children)))
                )
            case other =>
              JsError(s"expected kind to be parent, but was $other")
          },
      relation =>
        Json.obj(
          "kind"     -> "parent",
          "children" -> Json.toJson(relation.children)
        )
    )
}
