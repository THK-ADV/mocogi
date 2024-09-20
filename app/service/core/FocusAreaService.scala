package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.core.FocusAreaRepository
import models.core.FocusArea
import parser.Parser
import parsing.core.FocusAreaFileParser

@Singleton
final class FocusAreaService @Inject() (
    val repo: FocusAreaRepository,
    val studyProgramService: StudyProgramService,
    implicit val ctx: ExecutionContext
) extends AsyncParserYamlService[FocusArea] {
  override def parser: Future[Parser[List[FocusArea]]] =
    studyProgramService.allIds().map(FocusAreaFileParser.fileParser(_))
}
