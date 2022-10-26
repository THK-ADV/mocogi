package service

import basedata.StudyFormType
import database.repo.StudyFormTypeRepository
import parsing.base.StudyFormTypeFileParser

import javax.inject.{Inject, Singleton}

trait StudyFormTypeService extends SimpleYamlService[StudyFormType]

@Singleton
final class StudyFormTypeServiceImpl @Inject() (
    val repo: StudyFormTypeRepository
) extends StudyFormTypeService {
  override def parser = StudyFormTypeFileParser
}
