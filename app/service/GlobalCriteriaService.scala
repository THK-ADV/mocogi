package service

import basedata.GlobalCriteria
import database.repo.GlobalCriteriaRepository
import parsing.base.GlobalCriteriaFileParser

import javax.inject.{Inject, Singleton}

trait GlobalCriteriaService extends SimpleYamlService[GlobalCriteria]

@Singleton
final class GlobalCriteriaServiceImpl @Inject() (
    val repo: GlobalCriteriaRepository
) extends GlobalCriteriaService {
  override def parser = GlobalCriteriaFileParser
}
