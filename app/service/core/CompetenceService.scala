package service.core

import database.repo.core.CompetenceRepository
import models.core.ModuleCompetence
import parsing.core.{CompetenceFileParser, FileParser}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class CompetenceService @Inject() (
    val repo: CompetenceRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleCompetence] {
  override def fileParser: FileParser[ModuleCompetence] = CompetenceFileParser
}
