package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.core.CompetenceRepository
import models.core.ModuleCompetence
import parser.Parser
import parsing.core.CompetenceFileParser

@Singleton
final class CompetenceService @Inject() (
    val repo: CompetenceRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleCompetence] {
  override def fileParser: Parser[List[ModuleCompetence]] =
    CompetenceFileParser.parser()
}
