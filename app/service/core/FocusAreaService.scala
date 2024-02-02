package service.core

import database.repo.FocusAreaRepository
import models.core.FocusArea
import parsing.core.FocusAreaFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait FocusAreaService extends AsyncParserYamlService[FocusArea]

@Singleton
final class FocusAreaServiceImpl @Inject() (
    val repo: FocusAreaRepository,
    val studyProgramService: StudyProgramService,
    implicit val ctx: ExecutionContext
) extends FocusAreaService {
  override def parser =
    studyProgramService.allIds().map(FocusAreaFileParser.fileParser(_))
}
