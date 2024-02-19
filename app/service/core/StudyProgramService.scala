package service.core

import database.repo.core.StudyProgramRepository
import models.core.StudyProgram
import parser.Parser
import parsing.core.StudyProgramFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class StudyProgramService @Inject() (
    private val repo: StudyProgramRepository,
    private val degreeService: DegreeService,
    private val personService: IdentityService,
    implicit val ctx: ExecutionContext
) extends YamlService[StudyProgram] {

  override protected def parser: Future[Parser[List[StudyProgram]]] =
    for {
      degrees <- degreeService.all()
      people <- personService.all()
    } yield StudyProgramFileParser.fileParser(degrees, people)

  override def createOrUpdateMany(
      xs: Seq[StudyProgram]
  ): Future[Seq[StudyProgram]] = repo.createOrUpdateMany(xs)

  def all(): Future[Seq[StudyProgram]] =
    repo.all()

  def allIds() =
    repo.allIds()
}
