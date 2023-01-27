package controllers.formats

import database.{PrerequisiteEntryOutput, PrerequisitesOutput}
import play.api.libs.json.{Format, Json}

trait PrerequisitesOutputFormat extends JsonNullWritable {
  implicit val prerequisiteEntryOutputFormat: Format[PrerequisiteEntryOutput] =
    Json.format[PrerequisiteEntryOutput]

  implicit val prerequisitesOutputFormat: Format[PrerequisitesOutput] =
    Json.format[PrerequisitesOutput]
}
