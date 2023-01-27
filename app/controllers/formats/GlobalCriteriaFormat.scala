package controllers.formats

import basedata.GlobalCriteria
import play.api.libs.json.{Format, Json}

trait GlobalCriteriaFormat {
  implicit val globalCriteriaFormat: Format[GlobalCriteria] =
    Json.format[GlobalCriteria]
}
