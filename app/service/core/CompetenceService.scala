package service.core

import database.repo.core.CompetenceRepository
import models.core.ModuleCompetence
import parser.Parser
import parsing.core.CompetenceFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class CompetenceService @Inject() (
    val repo: CompetenceRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleCompetence] {
  override def fileParser: Parser[List[ModuleCompetence]] =
    CompetenceFileParser.parser()
}
