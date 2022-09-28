package providers

import parsing.metadata._

import javax.inject.{Inject, Provider, Singleton}

@Singleton()
final class MetadataParserProvider @Inject() (
    responsibilitiesParser: ResponsibilitiesParser,
    seasonParser: SeasonParser,
    statusParser: StatusParser,
    moduleTypeParser: ModuleTypeParser,
    locationParser: LocationParser,
    languageParser: LanguageParser
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
