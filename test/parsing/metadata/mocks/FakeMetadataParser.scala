package parsing.metadata.mocks

import models.core._
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
      identities: Seq[Identity],
      focusAreas: Seq[FocusAreaID],
      competences: Seq[Competence],
      globalCriteria: Seq[GlobalCriteria],
      pos: Seq[PO],
      specializations: Seq[Specialization]
  ) = always(
    ParsedMetadata(
      UUID.randomUUID(),
      "",
      "",
      ModuleType("", "", ""),
      None,
      Left(0),
      Language("", "", ""),
      0,
      Season("", "", ""),
      Responsibilities(Nil, Nil),
      AssessmentMethods(Nil, Nil),
      ParsedWorkload(0, 0, 0, 0, 0, 0),
      ParsedPrerequisites(None, None),
      Status("", "", ""),
      Location("", "", ""),
      ParsedPOs(Nil, Nil),
      None,
      Nil,
      Nil,
      Nil
    )
  )
}
