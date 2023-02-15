package service.core

import database.repo.StudyFormTypeRepository
import models.core.StudyFormType
import parsing.core.StudyFormTypeFileParser

import javax.inject.{Inject, Singleton}

trait StudyFormTypeService extends SimpleYamlService[StudyFormType]

@Singleton
final class StudyFormTypeServiceImpl @Inject() (
    val repo: StudyFormTypeRepository
) extends StudyFormTypeService {
  override def parser = StudyFormTypeFileParser
}
