package service.core

import database.repo.StudyFormTypeRepository
import models.core.StudyFormType
import parsing.core.StudyFormTypeFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait StudyFormTypeService extends SimpleYamlService[StudyFormType]

@Singleton
final class StudyFormTypeServiceImpl @Inject() (
    val repo: StudyFormTypeRepository,
    val ctx: ExecutionContext
) extends StudyFormTypeService {
  override def fileParser = StudyFormTypeFileParser
}
