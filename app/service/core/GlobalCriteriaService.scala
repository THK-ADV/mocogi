package service.core

import database.repo.core.GlobalCriteriaRepository
import models.core.ModuleGlobalCriteria
import parsing.core.GlobalCriteriaFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait GlobalCriteriaService extends SimpleYamlService[ModuleGlobalCriteria]

@Singleton
final class GlobalCriteriaServiceImpl @Inject() (
    val repo: GlobalCriteriaRepository,
    val ctx: ExecutionContext
) extends GlobalCriteriaService {
  override def fileParser = GlobalCriteriaFileParser
}
