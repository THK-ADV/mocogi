package controllers.formats

import models.core.Competence
import play.api.libs.json.{Format, Json}

trait CompetencesFormat {
  implicit val competenceFormat: Format[Competence] =
    Json.format[Competence]
}
