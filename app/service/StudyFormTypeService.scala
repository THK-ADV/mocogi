package service

import basedata.StudyFormType
import database.repo.StudyFormTypeRepository
import parsing.base.StudyFormTypeFileParser

import javax.inject.{Inject, Singleton}

trait StudyFormTypeService extends YamlService[StudyFormType, StudyFormType]

@Singleton
final class StudyFormTypeServiceImpl @Inject() (
    val repo: StudyFormTypeRepository
) extends StudyFormTypeService {
  override def parser = StudyFormTypeFileParser
  override def toInput(output: StudyFormType) = output
}
