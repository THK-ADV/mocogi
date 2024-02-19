package service.core

import database.repo.core.SpecializationRepository
import models.core.Specialization
import parser.Parser
import parsing.core.SpecializationFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class SpecializationService @Inject() (
    val repo: SpecializationRepository,
    private val poService: POService,
    implicit val ctx: ExecutionContext
) extends AsyncParserYamlService[Specialization] {

  override def parser: Future[Parser[List[Specialization]]] =
    poService.allIds().map(SpecializationFileParser.fileParser(_))
}
