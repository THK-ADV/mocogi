package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.core.AssessmentMethodRepository
import models.core.AssessmentMethod
import parser.Parser
import parsing.core.AssessmentMethodFileParser

@Singleton
final class AssessmentMethodService @Inject() (
    val repo: AssessmentMethodRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[AssessmentMethod] {
  override def fileParser: Parser[List[AssessmentMethod]] =
    AssessmentMethodFileParser.parser()
}
