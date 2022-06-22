package providers

import parsing.metadata.{LocationParser, LocationParserImpl}

import javax.inject.{Inject, Provider, Singleton}

@Singleton
class LocationParserProvider @Inject() (config: ConfigReader)
    extends Provider[LocationParser] {
  override def get() = new LocationParserImpl(config.locations)
}
