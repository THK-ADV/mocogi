package controllers.formats

import basedata.FocusArea
import play.api.libs.json.{Format, Json}

trait FocusAreaFormat {

  implicit val focusAreaFormat: Format[FocusArea] =
    Json.format[FocusArea]
}
