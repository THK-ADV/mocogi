package parsing.metadata

import models.core.{AssessmentMethod, Competence, FocusAreaPreview, GlobalCriteria, Language, Location, ModuleType, PO, Person, Season, Status}
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
      focusAreas: Seq[FocusAreaPreview],
      competences: Seq[Competence],
      globalCriteria: Seq[GlobalCriteria],
      pos: Seq[PO]
  ): Parser[ParsedMetadata]
}
