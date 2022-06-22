package providers

import parsing.metadata.{StatusParser, StatusParserImpl}

import javax.inject.{Inject, Provider, Singleton}

@Singleton
class StatusParserProvider @Inject() (config: ConfigReader)
    extends Provider[StatusParser] {
  override def get() = new StatusParserImpl(config.status)
}
