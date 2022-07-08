package service

import database.repo.AssessmentMethodRepository
import parsing.metadata.file.AssessmentMethodFileParser
import parsing.types.AssessmentMethod

import javax.inject.{Inject, Singleton}

trait AssessmentMethodService extends YamlService[AssessmentMethod]

@Singleton
final class AssessmentMethodServiceImpl @Inject() (
    val repo: AssessmentMethodRepository,
    val parser: AssessmentMethodFileParser
) extends AssessmentMethodService
