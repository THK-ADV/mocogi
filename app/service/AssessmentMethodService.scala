package service

import basedata.AssessmentMethod
import database.repo.AssessmentMethodRepository
import parsing.base.AssessmentMethodFileParser

import javax.inject.{Inject, Singleton}

trait AssessmentMethodService extends SimpleYamlService[AssessmentMethod]

@Singleton
final class AssessmentMethodServiceImpl @Inject() (
    val repo: AssessmentMethodRepository
) extends AssessmentMethodService {
  override def parser = AssessmentMethodFileParser
}
