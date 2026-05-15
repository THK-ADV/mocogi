package printing.latex.studyplan

import play.api.libs.json.Json
import play.api.libs.json.Reads

final case class StudyPlanSection(untilSemester: Int, headline: String)

object StudyPlanSection {
  given Reads[StudyPlanSection] = Json.reads[StudyPlanSection]
}