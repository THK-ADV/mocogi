package service.core

import database.repo.SpecializationRepository
import models.core.Specialization
import parser.Parser
import parsing.core.SpecializationFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait SpecializationService extends YamlService[Specialization, Specialization]

@Singleton
final class SpecializationServiceImpl @Inject() (
    val repo: SpecializationRepository,
    val poService: POService,
    implicit val ctx: ExecutionContext
) extends SpecializationService {

  override def parser: Future[Parser[List[Specialization]]] =
    poService.allIds().map(SpecializationFileParser.fileParser(_))

  override def toInput(output: Specialization): Specialization = output
}
