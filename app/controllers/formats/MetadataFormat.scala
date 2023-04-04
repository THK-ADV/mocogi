package controllers.formats

import models.core.FocusAreaPreview
import parsing.types._
import play.api.libs.json.{Format, Json}
import validator._

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
    with LocationFormat
    with ModuleRelationFormat
    with SpecializationFormat {
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

  implicit val metaDataFormat: Format[Metadata] =
    Json.format[Metadata]
}
