package service.core

import database.repo.GradeRepository
import models.core.Grade
import parsing.core.GradeFileParser

import javax.inject.{Inject, Singleton}

trait GradeService extends SimpleYamlService[Grade]

@Singleton
final class GradeServiceImpl @Inject() (
    val repo: GradeRepository
) extends GradeService {
  override def parser = GradeFileParser
}
