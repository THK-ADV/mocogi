package service.core

import database.repo.GlobalCriteriaRepository
import models.core.GlobalCriteria
import parsing.core.GlobalCriteriaFileParser

import javax.inject.{Inject, Singleton}

trait GlobalCriteriaService extends SimpleYamlService[GlobalCriteria]

@Singleton
final class GlobalCriteriaServiceImpl @Inject() (
    val repo: GlobalCriteriaRepository
) extends GlobalCriteriaService {
  override def parser = GlobalCriteriaFileParser
}
