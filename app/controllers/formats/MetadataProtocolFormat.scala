package controllers.formats

import models.MetadataProtocol
import parsing.types.ParsedWorkload
import play.api.libs.json.{Format, Json}

trait MetadataProtocolFormat
    extends ParticipantsFormat
    with ModuleRelationOutputFormat
    with AssessmentMethodsOutputFormat
    with PrerequisitesOutputFormat
    with POOutputFormat
    with JsonNullWritable {
  implicit val parsedWorkloadFormat: Format[ParsedWorkload] =
    Json.format[ParsedWorkload]

  implicit val metadataProtocolFormat: Format[MetadataProtocol] =
    Json.format[MetadataProtocol]
}
