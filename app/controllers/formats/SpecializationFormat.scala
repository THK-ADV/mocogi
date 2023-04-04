package controllers.formats

import models.core.Specialization
import play.api.libs.json.{Format, Json}

trait SpecializationFormat {
  implicit val specializationFormat: Format[Specialization] =
    Json.format[Specialization]
}
