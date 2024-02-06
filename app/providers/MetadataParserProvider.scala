package providers

import parsing.metadata._

import javax.inject.{Inject, Provider, Singleton}

@Singleton()
final class MetadataParserProvider @Inject() (
    responsibilitiesParser: ModuleResponsibilitiesParser,
    seasonParser: ModuleSeasonParser,
    statusParser: ModuleStatusParser,
    moduleTypeParser: ModuleTypeParser,
    locationParser: ModuleLocationParser,
    languageParser: ModuleLanguageParser
) extends Provider[Set[MetadataParser]] {
  override def get() = Set(
    new THKV1Parser(
      responsibilitiesParser,
      seasonParser,
      statusParser,
      moduleTypeParser,
      locationParser,
      languageParser
    )
  )
}
