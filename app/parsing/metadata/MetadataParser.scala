package parsing.metadata

import models.core._
import parser.Parser
import parsing.types._

trait MetadataParser {
  val versionScheme: VersionScheme
  def parser(implicit
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
  ): Parser[ParsedMetadata]
}
