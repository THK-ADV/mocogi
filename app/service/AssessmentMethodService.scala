package service

import basedata.AssessmentMethod
import database.repo.AssessmentMethodRepository
import parsing.base.AssessmentMethodFileParser

import javax.inject.{Inject, Singleton}

trait AssessmentMethodService
    extends YamlService[AssessmentMethod, AssessmentMethod]

@Singleton
final class AssessmentMethodServiceImpl @Inject() (
    val repo: AssessmentMethodRepository,
    val parser: AssessmentMethodFileParser
) extends AssessmentMethodService {
  override def toInput(output: AssessmentMethod) = output
}
