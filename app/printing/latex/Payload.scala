package printing.latex

import models.core.*
import models.ModuleCore
import models.StudyProgramView

case class Payload(
    studyProgram: StudyProgramView,
    moduleTypes: Seq[ModuleType],
    languages: Seq[ModuleLanguage],
    seasons: Seq[Season],
    people: Seq[Identity],
    assessmentMethods: Seq[AssessmentMethod],
    studyProgramViews: Seq[StudyProgramView],
    modules: Seq[ModuleCore]
)
