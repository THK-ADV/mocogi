package controllers.formats

import basedata.FocusAreaPreview
import parsing.types._
import play.api.libs.json.{Format, JsError, Json, OFormat}
import validator.{
  Metadata,
  Module,
  ModuleRelation,
  POOptional,
  POs,
  PrerequisiteEntry,
  Prerequisites
}

trait MetadataFormat
    extends JsonNullWritable
    with ModuleFormat
    with ModuleTypeFormat
    with LanguageFormat
    with SeasonFormat
    with PersonFormat
    with AssessmentMethodFormat
    with WorkloadFormat
    with ParticipantsFormat
    with CompetencesFormat
    with GlobalCriteriaFormat
    with POFormat
    with StatusFormat
    with LocationFormat {
  implicit val poMandatoryFormat: Format[POMandatory] =
    Json.format[POMandatory]

  implicit val poOptFormat: Format[POOptional] =
    Json.format[POOptional]

  implicit val posFormat: Format[POs] =
    Json.format[POs]

  implicit val prerequisitesEntryFormat: Format[PrerequisiteEntry] =
    Json.format[PrerequisiteEntry]

  implicit val prerequisitesFormat: Format[Prerequisites] =
    Json.format[Prerequisites]

  implicit val assessmentMethodEntry: Format[AssessmentMethodEntry] =
    Json.format[AssessmentMethodEntry]

  implicit val assessmentMethods: Format[AssessmentMethods] =
    Json.format[AssessmentMethods]

  implicit val responsibilitiesFormat: Format[Responsibilities] =
    Json.format[Responsibilities]

  implicit val focusAreaPreviewFormat: Format[FocusAreaPreview] =
    Json.format[FocusAreaPreview]

  implicit val ectsFocusAreaContribution: Format[ECTSFocusAreaContribution] =
    Json.format[ECTSFocusAreaContribution]

  implicit val ectsFormat: Format[ECTS] =
    Json.format[ECTS]

  implicit val moduleRelationFormat: Format[ModuleRelation] =
    OFormat.apply(
      js =>
        js.\("type")
          .validate[String]
          .flatMap {
            case "parent" =>
              js.\("children")
                .validate[List[Module]]
                .map(ModuleRelation.Parent.apply)
            case "child" =>
              js.\("parent").validate[Module].map(ModuleRelation.Child.apply)
            case other =>
              JsError(s"expected type to be parent or child, but was $other")
          },
      {
        case ModuleRelation.Parent(children) =>
          Json.obj(
            "type" -> "parent",
            "children" -> Json.toJson(children)
          )
        case ModuleRelation.Child(parent) =>
          Json.obj(
            "type" -> "child",
            "parent" -> Json.toJson(parent)
          )
      }
    )

  implicit val metaDataFormat: Format[Metadata] =
    Json.format[Metadata]
}