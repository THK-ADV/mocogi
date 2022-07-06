package controllers.json

import parsing.types.AssessmentMethod
import play.api.libs.json.{Format, Json}

trait AssessmentMethodFormat {
  implicit val assessmentMethodFormat: Format[AssessmentMethod] =
    Json.format[AssessmentMethod]
}
