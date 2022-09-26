package controllers.json

import parsing.types.ModuleRelation.{Child, Parent}
import parsing.types._
import play.api.libs.json.{Format, JsError, Json, OFormat}

trait MetadataFormat
    extends ModuleTypeFormat
    with LanguageFormat
    with SeasonFormat
    with AssessmentMethodFormat
    with PersonFormat
    with StatusFormat
    with LocationFormat
    with ECTSFormat
    with PrerequisitesFormat {

  implicit val workloadFormat: Format[Workload] =
    Json.format[Workload]

  implicit val assessmentMethodPercentFormat
      : Format[AssessmentMethodPercentage] =
    Json.format[AssessmentMethodPercentage]

  implicit val responsibilitiesFormat: Format[Responsibilities] =
    Json.format[Responsibilities]

  implicit val moduleRelationFormat: Format[ModuleRelation] =
    OFormat.apply(
      js =>
        js.\("type").validate[String].flatMap {
          case "parent" =>
            js.\("children").validate[List[String]].map(Parent.apply)
          case "child" =>
            js.\("parent").validate[String].map(Child.apply)
          case other =>
            JsError(s"expected type to be parent or child, but was $other")
        },
      {
        case Parent(children) =>
          Json.obj(
            "type" -> "parent",
            "children" -> Json.toJson(children)
          )
        case Child(parent) =>
          Json.obj(
            "type" -> "child",
            "parent" -> Json.toJson(parent)
          )
      }
    )

  implicit val metaDataFormat: Format[Metadata] =
    Json.format[Metadata]
}
