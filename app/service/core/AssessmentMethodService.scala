package service.core

import database.repo.core.AssessmentMethodRepository
import models.core.AssessmentMethod
import parsing.core.AssessmentMethodFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

// TODO inline
trait AssessmentMethodService extends SimpleYamlService[AssessmentMethod]

@Singleton
final class AssessmentMethodServiceImpl @Inject() (
    val repo: AssessmentMethodRepository,
    val ctx: ExecutionContext
) extends AssessmentMethodService {
  override def fileParser = AssessmentMethodFileParser
}
