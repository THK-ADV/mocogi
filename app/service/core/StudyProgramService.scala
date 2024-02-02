package service.core

import database.repo.StudyProgramRepository
import models.core.StudyProgram
import parser.Parser
import parsing.core.StudyProgramFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait StudyProgramService extends YamlService[StudyProgram] {
  def allIds(): Future[Seq[String]]
}

@Singleton
final class StudyProgramServiceImpl @Inject() (
    val repo: StudyProgramRepository,
    val degreeService: DegreeService,
    val personService: IdentityService,
    implicit val ctx: ExecutionContext
) extends StudyProgramService {

  override protected def parser: Future[Parser[List[StudyProgram]]] =
    for {
      degrees <- degreeService.all()
      people <- personService.all()
    } yield StudyProgramFileParser.fileParser(degrees, people)

  override def createOrUpdateMany(
      xs: Seq[StudyProgram]
  ): Future[Seq[StudyProgram]] = repo.createOrUpdateMany(xs)

  override def all(): Future[Seq[StudyProgram]] =
    repo.all()

  override def allIds() =
    repo.allIds()
}
