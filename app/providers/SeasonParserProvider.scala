package providers

import parsing.metadata.{SeasonParser, SeasonParserImpl}

import javax.inject.{Inject, Provider, Singleton}

@Singleton
final class SeasonParserProvider @Inject() (config: ConfigReader)
    extends Provider[SeasonParser] {
  override def get() = new SeasonParserImpl(config.seasons)
}
