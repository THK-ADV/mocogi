package service.core

import database.repo.core.GlobalCriteriaRepository
import models.core.ModuleGlobalCriteria
import parsing.core.GlobalCriteriaFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class GlobalCriteriaService @Inject() (
    val repo: GlobalCriteriaRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleGlobalCriteria] {
  override def fileParser = GlobalCriteriaFileParser
}
