package parsing.metadata.mocks

import parser.Parser.always
import parsing.metadata.{MetadataParser, VersionScheme}
import parsing.types._

import java.util.UUID

class FakeMetadataParser extends MetadataParser {
  override val versionScheme = VersionScheme(1, "s")

  override def parser(implicit
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
  ) = always(
    Metadata(
      UUID.randomUUID(),
      "",
      "",
      ModuleType("", "", ""),
      None,
      ECTS(0, Nil),
      Language("", "", ""),
      0,
      Season("", "", ""),
      Responsibilities(Nil, Nil),
      AssessmentMethods(Nil, Nil),
      Workload(0, 0, 0, 0, 0, 0),
      Prerequisites(None, None),
      Status("", "", ""),
      Location("", "", ""),
      POs(Nil, Nil),
      None,
      Nil,
      Nil,
      Nil
    )
  )
}
