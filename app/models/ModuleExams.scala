package models

import play.api.libs.json.__
import play.api.libs.json.Format

/** Exam names of a module. `None` selects the default exam named after the module. */
case class ModuleExams(examNames: Option[List[String]])

object ModuleExams {
  given Format[ModuleExams] =
    (__ \ "examNames").format(Format.optionWithNull[List[String]]).bimap(ModuleExams.apply, _.examNames)
}
