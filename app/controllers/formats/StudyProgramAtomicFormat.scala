package controllers.formats

import database.view.{SpecializationShort, StudyProgramAtomic}
import play.api.libs.json.{Format, Json}

trait StudyProgramAtomicFormat extends JsonNullWritable {
  implicit val specializationShortFmt: Format[SpecializationShort] =
    Json.format[SpecializationShort]

  implicit val studyProgramAtomicFmt: Format[StudyProgramAtomic] =
    Json.format[StudyProgramAtomic]
}
