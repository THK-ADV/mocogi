package service.core

import database.repo.PORepository
import models.core.PO
import parsing.core.POFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait POService extends YamlService[PO, PO] {
  def allValid(): Future[Seq[PO]]
  def allIds(): Future[Seq[String]]
}

@Singleton
final class POServiceImpl @Inject() (
    val repo: PORepository,
    val studyProgramService: StudyProgramService,
    implicit val ctx: ExecutionContext
) extends POService {

  override def parser =
    studyProgramService.allIds().map(POFileParser.fileParser(_))

  override def allIds() =
    repo.allIds()

  override def allValid() =
    repo.allValid()

  override def toInput(output: PO): PO = output
}
