package service

import basedata.Grade
import database.repo.GradeRepository
import parsing.base.GradeFileParser

import javax.inject.{Inject, Singleton}

trait GradeService extends SimpleYamlService[Grade]

@Singleton
final class GradeServiceImpl @Inject() (
    val repo: GradeRepository
) extends GradeService {
  override def parser = GradeFileParser
}
