package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.core.SpecializationRepository
import models.core.Specialization
import parser.Parser
import parsing.core.SpecializationFileParser

@Singleton
final class SpecializationService @Inject() (
    val repo: SpecializationRepository,
    private val poService: POService,
    implicit val ctx: ExecutionContext
) extends AsyncParserYamlService[Specialization] {

  override def parser: Future[Parser[List[Specialization]]] =
    poService.allIds().map(SpecializationFileParser.fileParser(_))
}
