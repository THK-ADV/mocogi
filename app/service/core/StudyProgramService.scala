package service.core

import database.InsertOrUpdateResult
import database.repo.StudyProgramRepository
import models.{StudyProgramOutput, StudyProgramShort}
import models.core.StudyProgram
import parsing.core.StudyProgramFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait StudyProgramService {
  def all(): Future[Seq[StudyProgramOutput]]
  def allShort(): Future[Seq[StudyProgramShort]]
  def allIds(): Future[Seq[String]]
  def create(input: String): Future[List[StudyProgram]]
  def createOrUpdate(
      input: String
  ): Future[List[(InsertOrUpdateResult, StudyProgram)]]
}

@Singleton
final class StudyProgramServiceImpl @Inject() (
    val repo: StudyProgramRepository,
    val gradeService: GradeService,
    val personService: IdentityService,
    val studyFormTypeService: StudyFormTypeService,
    val languageService: LanguageService,
    val seasonService: SeasonService,
    val locationService: LocationService,
    implicit val ctx: ExecutionContext
) extends StudyProgramService {

  def all(): Future[Seq[StudyProgramOutput]] =
    repo.all()

  override def allShort() =
    repo.allShort()

  override def allIds() =
    repo.allIds()

  def create(input: String): Future[List[StudyProgram]] =
    createFromInput(input, xs => repo.createMany(xs).map(_ => xs))

  def createOrUpdate(
      input: String
  ): Future[List[(InsertOrUpdateResult, StudyProgram)]] =
    createFromInput(
      input,
      xs =>
        Future.sequence(
          xs.map(sp =>
            repo.exists(sp).flatMap {
              case true => repo.update(sp).map(InsertOrUpdateResult.Update -> _)
              case false =>
                repo.create(sp).map(InsertOrUpdateResult.Insert -> _)
            }
          )
        )
    )

  private def createFromInput[A](
      input: String,
      createMany: List[StudyProgram] => Future[List[A]]
  ): Future[List[A]] =
    for {
      grades <- gradeService.all()
      people <- personService.all()
      studyFormTypes <- studyFormTypeService.all()
      languages <- languageService.all()
      seasons <- seasonService.all()
      locations <- locationService.all()
      programs <- StudyProgramFileParser
        .fileParser(
          grades,
          people,
          studyFormTypes,
          languages,
          seasons,
          locations
        )
        .parse(input)
        ._1
        .fold(Future.failed, createMany)
    } yield programs
}
