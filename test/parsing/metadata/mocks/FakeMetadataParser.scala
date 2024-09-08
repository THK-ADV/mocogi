package parsing.metadata.mocks

import cats.data.NonEmptyList
import models.Examiner
import models.core._
import parser.Parser.always
import parsing.metadata.{MetadataParser, VersionScheme}
import parsing.types._

import java.util.UUID

class FakeMetadataParser extends MetadataParser {
  override val versionScheme = VersionScheme(1, "s")

  override def parser(implicit
      locations: Seq[ModuleLocation],
      languages: Seq[ModuleLanguage],
      status: Seq[ModuleStatus],
      assessmentMethods: Seq[AssessmentMethod],
      moduleTypes: Seq[ModuleType],
      seasons: Seq[Season],
      identities: Seq[Identity],
      focusAreas: Seq[FocusAreaID],
      competences: Seq[ModuleCompetence],
      globalCriteria: Seq[ModuleGlobalCriteria],
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
      ModuleLanguage("", "", ""),
      0,
      Season("", "", ""),
      ModuleResponsibilities(
        NonEmptyList.one(Identity.Unknown("id", "label")),
        NonEmptyList.one(Identity.Unknown("id", "label"))
      ),
      ModuleAssessmentMethods(Nil, Nil),
      Examiner(Identity.NN, Identity.NN),
      ExamPhase.all,
      ParsedWorkload(0, 0, 0, 0, 0, 0),
      ParsedPrerequisites(None, None),
      ModuleStatus("", "", ""),
      ModuleLocation("", "", ""),
      ParsedPOs(Nil, Nil),
      None,
      Nil,
      Nil,
      Nil
    )
  )
}
