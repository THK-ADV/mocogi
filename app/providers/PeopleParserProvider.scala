package providers

import parsing.metadata.{PeopleParser, PeopleParserImpl}

import javax.inject.{Inject, Provider, Singleton}

@Singleton
class PeopleParserProvider @Inject() (config: ConfigReader)
    extends Provider[PeopleParser] {
  override def get() = new PeopleParserImpl(config.persons)
}
