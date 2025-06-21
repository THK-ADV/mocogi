package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.core.PORepository
import models.core.PO
import parsing.core.POFileParser

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

  def allWithIds(pos: List[String]) =
    repo.allWithIds(pos)
}
