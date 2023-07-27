package controllers.formats

import database.view.StudyProgramAtomic
import play.api.libs.json.{Format, Json}

trait StudyProgramAtomicFormat extends JsonNullWritable {
  implicit val studyProgramAtomicFmt: Format[StudyProgramAtomic] =
    Json.format[StudyProgramAtomic]
}
