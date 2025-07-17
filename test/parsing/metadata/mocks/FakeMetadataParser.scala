package parsing.metadata.mocks

import java.util.UUID

import cats.data.NonEmptyList
import models.core.*
import models.core.ExamPhases.ExamPhase
import models.Examiner
import models.ModuleWorkload
import parser.Parser.always
import parsing.metadata.MetadataParser
import parsing.metadata.VersionScheme
import parsing.types.*

class FakeMetadataParser extends MetadataParser {
  override val versionScheme = VersionScheme(1, "s")

  override def parser(
      implicit locations: Seq[ModuleLocation],
      languages: Seq[ModuleLanguage],
      status: Seq[ModuleStatus],
      assessmentMethods: Seq[AssessmentMethod],
      moduleTypes: Seq[ModuleType],
      seasons: Seq[Season],
      identities: Seq[Identity],
      pos: Seq[PO],
      specializations: Seq[Specialization]
  ) = always(
    ParsedMetadata(
      UUID.randomUUID(),
      "",
      "",
      ModuleType("", "", ""),
      None,
      0,
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
      ModuleWorkload(0, 0, 0, 0, 0, 0),
      ParsedPrerequisites(None, None),
      ModuleStatus("", "", ""),
      ModuleLocation("", "", ""),
      ParsedPOs(Nil, Nil),
      None,
      Nil,
      Nil,
      Nil,
      None,
      None
    )
  )
}
