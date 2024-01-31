package parsing.metadata

import models.core._
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
      identities: Seq[Identity],
      focusAreas: Seq[FocusAreaPreview],
      competences: Seq[Competence],
      globalCriteria: Seq[GlobalCriteria],
      pos: Seq[PO],
      specializations: Seq[Specialization]
  ): Parser[ParsedMetadata]
}
