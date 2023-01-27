package controllers.formats

import database.ModuleRelationOutput
import play.api.libs.json.{Format, JsError, Json, OFormat}

import java.util.UUID

trait ModuleRelationOutputFormat {
  implicit val moduleRelationOutputFormat: Format[ModuleRelationOutput] =
    OFormat.apply(
      js =>
        js.\("type")
          .validate[String]
          .flatMap {
            case "parent" =>
              js.\("children")
                .validate[List[UUID]]
                .map(ModuleRelationOutput.Parent.apply)
            case "child" =>
              js.\("parent")
                .validate[UUID]
                .map(ModuleRelationOutput.Child.apply)
            case other =>
              JsError(s"expected type to be parent or child, but was $other")
          },
      {
        case ModuleRelationOutput.Parent(children) =>
          Json.obj(
            "type" -> "parent",
            "children" -> Json.toJson(children)
          )
        case ModuleRelationOutput.Child(parent) =>
          Json.obj(
            "type" -> "child",
            "parent" -> Json.toJson(parent)
          )
      }
    )
}
