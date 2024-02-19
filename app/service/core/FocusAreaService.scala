package service.core

import database.repo.core.FocusAreaRepository
import models.core.FocusArea
import parsing.core.FocusAreaFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class FocusAreaService @Inject() (
    val repo: FocusAreaRepository,
    val studyProgramService: StudyProgramService,
    implicit val ctx: ExecutionContext
) extends AsyncParserYamlService[FocusArea] {
  override def parser =
    studyProgramService.allIds().map(FocusAreaFileParser.fileParser(_))
}
