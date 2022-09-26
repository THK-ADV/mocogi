package controllers.json

import parsing.types.GlobalCriteria
import play.api.libs.json.{Format, Json}

trait GlobalCriteriaFormat {
  implicit val globalCriteriaFormat: Format[GlobalCriteria] =
    Json.format[GlobalCriteria]
}
