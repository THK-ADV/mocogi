package controllers.formats

import database.{AssessmentMethodEntryOutput, AssessmentMethodsOutput}
import play.api.libs.json.{Format, Json}

trait AssessmentMethodsOutputFormat extends JsonNullWritable {
  implicit val assessmentMethodEntryOutputFormat
      : Format[AssessmentMethodEntryOutput] =
    Json.format[AssessmentMethodEntryOutput]

  implicit val assessmentMethodsOutputFormat: Format[AssessmentMethodsOutput] =
    Json.format[AssessmentMethodsOutput]
}
