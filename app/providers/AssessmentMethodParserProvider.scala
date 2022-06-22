package providers

import parsing.metadata.{AssessmentMethodParser, AssessmentMethodParserImpl}

import javax.inject.{Inject, Provider, Singleton}

@Singleton
final class AssessmentMethodParserProvider @Inject() (config: ConfigReader)
    extends Provider[AssessmentMethodParser] {
  override def get() = new AssessmentMethodParserImpl(config.assessmentMethods)
}
