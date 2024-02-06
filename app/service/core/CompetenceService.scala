package service.core

import database.repo.core.CompetenceRepository
import models.core.Competence
import parsing.core.{CompetenceFileParser, FileParser}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait CompetenceService extends SimpleYamlService[Competence]

@Singleton
final class CompetenceServiceImpl @Inject() (
    val repo: CompetenceRepository,
    val ctx: ExecutionContext
) extends CompetenceService {
  override def fileParser: FileParser[Competence] = CompetenceFileParser
}
