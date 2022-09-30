package parsing.metadata

import basedata.{AssessmentMethod, Competence, FocusArea, GlobalCriteria, Language, Location, ModuleType, Person, Season, Status, StudyProgram}
import parser.Parser
import parsing.types._

trait MetadataParser {
  val versionScheme: VersionScheme
  def parser(implicit
      locations: Seq[Location],
      languages: Seq[Language],
      status: Seq[Status],
      assessmentMethods: Seq[AssessmentMethod],
      moduleTypes: Seq[ModuleType],
      seasons: Seq[Season],
      persons: Seq[Person],
      focusAreas: Seq[FocusArea],
      competences: Seq[Competence],
      globalCriteria: Seq[GlobalCriteria],
      studyPrograms: Seq[StudyProgram]
  ): Parser[Metadata]
}
