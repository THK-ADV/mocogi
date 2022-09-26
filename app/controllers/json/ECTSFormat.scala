package controllers.json

import parsing.types.{ECTS, ECTSFocusAreaContribution}
import play.api.libs.json.{Format, Json}

trait ECTSFormat extends FocusAreaFormat {
  implicit val ectsFocusAreaContribution: Format[ECTSFocusAreaContribution] =
    Json.format[ECTSFocusAreaContribution]

  implicit val ectsFormat: Format[ECTS] =
    Json.format[ECTS]
}
