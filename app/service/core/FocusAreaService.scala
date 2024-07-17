package service.core

import database.repo.core.FocusAreaRepository
import models.core.FocusArea
import parser.Parser
import parsing.core.FocusAreaFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class FocusAreaService @Inject() (
    val repo: FocusAreaRepository,
    val studyProgramService: StudyProgramService,
    implicit val ctx: ExecutionContext
) extends AsyncParserYamlService[FocusArea] {
  override def parser: Future[Parser[List[FocusArea]]] =
    studyProgramService.allIds().map(FocusAreaFileParser.fileParser(_))
}
