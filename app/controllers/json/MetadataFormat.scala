package controllers.json

import basedata.FocusAreaPreview
import database.repo._
import parsing.types._
import play.api.libs.json.{Format, JsError, Json, OFormat}
import validator.ModuleRelation.{Child, Parent}
import validator.{
  Metadata,
  Module,
  ModuleRelation,
  POOptional,
  POs,
  PrerequisiteEntry,
  Prerequisites,
  Workload
}

trait MetadataFormat
    extends ModuleTypeFormat
    with LanguageFormat
    with SeasonFormat
    with AssessmentMethodFormat
    with PersonFormat
    with StatusFormat
    with LocationFormat
    with CompetencesFormat
    with GlobalCriteriaFormat
    with FocusAreaFormat
    with POFormat
    with JsonNullWritable {

  implicit val participantsFormat: Format[Participants] =
    Json.format[Participants]

  implicit val poMandatoryFormat: Format[POMandatory] =
    Json.format[POMandatory]

  implicit val focusAreaPreviewFormat: Format[FocusAreaPreview] =
    Json.format[FocusAreaPreview]

  implicit val ectsFocusAreaContribution: Format[ECTSFocusAreaContribution] =
    Json.format[ECTSFocusAreaContribution]

  implicit val ectsFormat: Format[ECTS] =
    Json.format[ECTS]

  implicit val workloadFormat: Format[Workload] =
    Json.format[Workload]

  implicit val assessmentMethodEntry: Format[AssessmentMethodEntry] =
    Json.format[AssessmentMethodEntry]

  implicit val assessmentMethods: Format[AssessmentMethods] =
    Json.format[AssessmentMethods]

  implicit val responsibilitiesFormat: Format[Responsibilities] =
    Json.format[Responsibilities]

  implicit val moduleFormat: Format[Module] =
    Json.format[Module]

  implicit val prerequisitesEntryFormat: Format[PrerequisiteEntry] =
    Json.format[PrerequisiteEntry]

  implicit val prerequisitesFormat: Format[Prerequisites] =
    Json.format[Prerequisites]

  implicit val poOptFormat: Format[POOptional] =
    Json.format[POOptional]

  implicit val posFormat: Format[POs] =
    Json.format[POs]

  implicit val moduleRelationFormat: Format[ModuleRelation] =
    OFormat.apply(
      js =>
        js.\("type").validate[String].flatMap {
          case "parent" =>
            js.\("children").validate[List[Module]].map(Parent.apply)
          case "child" =>
            js.\("parent").validate[Module].map(Child.apply)
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

  implicit val assessmentMethodEntryOutputFormat
      : Format[AssessmentMethodEntryOutput] =
    Json.format[AssessmentMethodEntryOutput]

  implicit val prerequisiteEntryOutputFormat: Format[PrerequisiteEntryOutput] =
    Json.format[PrerequisiteEntryOutput]

  implicit val poMandatoryOutputFormat: Format[POMandatoryOutput] =
    Json.format[POMandatoryOutput]

  implicit val poOptionalOutputFormat: Format[POOptionalOutput] =
    Json.format[POOptionalOutput]

  implicit val assessmentMethodsOutputFormat: Format[AssessmentMethodsOutput] =
    Json.format[AssessmentMethodsOutput]

  implicit val prerequisitesOutputFormat: Format[PrerequisitesOutput] =
    Json.format[PrerequisitesOutput]

  implicit val poOutputFormat: Format[POOutput] =
    Json.format[POOutput]

  implicit val metaDataOutputFormat: Format[MetadataOutput] =
    Json.format[MetadataOutput]
}
