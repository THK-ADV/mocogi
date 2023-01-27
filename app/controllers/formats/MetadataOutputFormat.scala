package controllers.formats

import database.MetadataOutput
import play.api.libs.json.{Format, Json}

trait MetadataOutputFormat
    extends WorkloadFormat
    with ParticipantsFormat
    with ModuleRelationOutputFormat
    with AssessmentMethodsOutputFormat
    with PrerequisitesOutputFormat
    with POOutputFormat
    with JsonNullWritable {
  implicit val metadataOutputFormat: Format[MetadataOutput] =
    Json.format[MetadataOutput]
}
