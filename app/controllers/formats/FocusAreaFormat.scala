package controllers.formats

import models.core.FocusArea
import play.api.libs.json.{Format, Json}

trait FocusAreaFormat {

  implicit val focusAreaFormat: Format[FocusArea] =
    Json.format[FocusArea]
}
