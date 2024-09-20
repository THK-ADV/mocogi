package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.core.GlobalCriteriaRepository
import models.core.ModuleGlobalCriteria
import parser.Parser
import parsing.core.GlobalCriteriaFileParser

@Singleton
final class GlobalCriteriaService @Inject() (
    val repo: GlobalCriteriaRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleGlobalCriteria] {
  override def fileParser: Parser[List[ModuleGlobalCriteria]] =
    GlobalCriteriaFileParser.parser()
}
