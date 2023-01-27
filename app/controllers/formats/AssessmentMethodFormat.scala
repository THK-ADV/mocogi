package controllers.formats

import basedata.AssessmentMethod
import play.api.libs.json.{Format, Json}

trait AssessmentMethodFormat {
  implicit val assessmentMethodFormat: Format[AssessmentMethod] =
    Json.format[AssessmentMethod]
}
