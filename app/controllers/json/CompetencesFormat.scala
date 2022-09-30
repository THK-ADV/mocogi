package controllers.json

import basedata.Competence
import play.api.libs.json.{Format, Json}

trait CompetencesFormat {
  implicit val competenceFormat: Format[Competence] =
    Json.format[Competence]
}
