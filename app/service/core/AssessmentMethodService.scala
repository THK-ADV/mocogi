package service.core

import database.repo.AssessmentMethodRepository
import models.core.AssessmentMethod
import parsing.core.AssessmentMethodFileParser

import javax.inject.{Inject, Singleton}

trait AssessmentMethodService extends SimpleYamlService[AssessmentMethod]

@Singleton
final class AssessmentMethodServiceImpl @Inject() (
    val repo: AssessmentMethodRepository
) extends AssessmentMethodService {
  override def parser = AssessmentMethodFileParser
}
