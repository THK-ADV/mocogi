package printing.latex

import models.core._
import models.ModuleProtocol
import models.StudyProgramView

case class Payload(
    studyProgram: StudyProgramView,
    entries: Seq[ModuleProtocol],
    moduleTypes: Seq[ModuleType],
    languages: Seq[ModuleLanguage],
    seasons: Seq[Season],
    people: Seq[Identity],
    assessmentMethods: Seq[AssessmentMethod],
    studyProgramViews: Seq[StudyProgramView]
)
