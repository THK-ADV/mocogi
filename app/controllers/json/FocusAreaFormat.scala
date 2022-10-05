package controllers.json

import basedata.FocusAreaPreview
import play.api.libs.json.{Format, Json}

trait FocusAreaFormat {
  implicit val focusAreaFormat: Format[FocusAreaPreview] =
    Json.format[FocusAreaPreview]
}
