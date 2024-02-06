package service.core

import database.repo.core.PORepository
import models.core.PO
import parsing.core.POFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class POService @Inject() (
    val repo: PORepository,
    val studyProgramService: StudyProgramService,
    implicit val ctx: ExecutionContext
) extends AsyncParserYamlService[PO] {

  override def parser =
    studyProgramService.allIds().map(POFileParser.fileParser(_))

  def allIds() =
    repo.allIds()

  def allValid() =
    repo.allValid()
}
