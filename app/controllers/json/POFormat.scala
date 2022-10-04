package controllers.json

import basedata.{PO, StudyProgramPreview}
import play.api.libs.json.{Format, Json}

trait POFormat {
  implicit val poFormat: Format[PO] =
    Json.format[PO]

  implicit val spPreviewFormat: Format[StudyProgramPreview] =
    Json.format[StudyProgramPreview]
}
