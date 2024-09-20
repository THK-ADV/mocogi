package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.core.StudyProgramRepository
import models.core.StudyProgram
import parser.Parser
import parsing.core.StudyProgramFileParser

@Singleton
final class StudyProgramService @Inject() (
    private val repo: StudyProgramRepository,
    private val degreeService: DegreeService,
    private val personService: IdentityService,
    implicit val ctx: ExecutionContext
) extends YamlService[StudyProgram] {

  override def parser: Future[Parser[List[StudyProgram]]] =
    for {
      degrees <- degreeService.all()
      people  <- personService.all()
    } yield StudyProgramFileParser.fileParser(degrees, people)

  override def createOrUpdateMany(
      xs: Seq[StudyProgram]
  ): Future[Seq[StudyProgram]] =
    repo.createOrUpdateMany(xs)

  def all(): Future[Seq[StudyProgram]] =
    repo.all()

  def allIds() =
    repo.allIds()

  def deleteMany(ids: Seq[String]) =
    repo.deleteMany(ids)
}
