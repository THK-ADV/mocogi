package controllers.formats

import play.api.libs.json.{Format, JsError, Json, OFormat}
import validator.{Module, ModuleRelation}

trait ModuleRelationFormat extends ModuleFormat {
  implicit val moduleRelationFormat: Format[ModuleRelation] =
    OFormat.apply(
      js =>
        js.\("kind")
          .validate[String]
          .flatMap {
            case "parent" =>
              js.\("children")
                .validate[List[Module]]
                .map(ModuleRelation.Parent.apply)
            case "child" =>
              js.\("parent").validate[Module].map(ModuleRelation.Child.apply)
            case other =>
              JsError(s"expected kind to be parent or child, but was $other")
          },
      {
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
    )
}
