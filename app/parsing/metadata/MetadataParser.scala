package parsing.metadata

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
      persons: Seq[Person]
  ): Parser[Metadata]
}
