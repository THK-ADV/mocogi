package printing.latex

import models.core.*
import models.ModuleCore
import models.StudyProgramView

case class Payload(
    moduleTypes: Seq[ModuleType],
    languages: Seq[ModuleLanguage],
    seasons: Seq[Season],
    people: Seq[Identity],
    assessmentMethods: Seq[AssessmentMethod],
    studyPrograms: Seq[StudyProgramView],
    modules: Seq[ModuleCore]
)
