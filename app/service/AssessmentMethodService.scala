package service

import basedata.AssessmentMethod
import database.repo.AssessmentMethodRepository
import parsing.metadata.file.AssessmentMethodFileParser

import javax.inject.{Inject, Singleton}

trait AssessmentMethodService extends YamlService[AssessmentMethod]

@Singleton
final class AssessmentMethodServiceImpl @Inject() (
    val repo: AssessmentMethodRepository,
    val parser: AssessmentMethodFileParser
) extends AssessmentMethodService
