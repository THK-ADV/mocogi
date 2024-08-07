package service.core

import database.repo.core.AssessmentMethodRepository
import models.core.AssessmentMethod
import parser.Parser
import parsing.core.AssessmentMethodFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class AssessmentMethodService @Inject() (
    val repo: AssessmentMethodRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[AssessmentMethod] {
  override def fileParser: Parser[List[AssessmentMethod]] =
    AssessmentMethodFileParser.parser()
}
