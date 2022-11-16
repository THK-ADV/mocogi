package service

import basedata.StudyProgram
import database.InsertOrUpdateResult
import database.repo.{StudyProgramOutput, StudyProgramRepository}
import parsing.base.StudyProgramFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait StudyProgramService {
  def all(): Future[Seq[StudyProgramOutput]]
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
    val personService: PersonService,
    val studyFormTypeService: StudyFormTypeService,
    val languageService: LanguageService,
    val seasonService: SeasonService,
    val locationService: LocationService,
    implicit val ctx: ExecutionContext
) extends StudyProgramService {

  def all(): Future[Seq[StudyProgramOutput]] =
    repo.all()

  override def allIds() =
    repo.allIds()

  def create(input: String): Future[List[StudyProgram]] =
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
        .fold(Future.failed, xs => repo.createMany(xs).map(_ => xs))
    } yield programs

  def createOrUpdate(
      input: String
  ): Future[List[(InsertOrUpdateResult, StudyProgram)]] = {
    def go(xs: List[StudyProgram]) =
      Future.sequence(
        xs.map(sp =>
          repo.exists(sp).flatMap {
            case true  => repo.update(sp).map(InsertOrUpdateResult.Update -> _)
            case false => repo.create(sp).map(InsertOrUpdateResult.Insert -> _)
          }
        )
      )

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
        .fold(Future.failed, go)
    } yield programs
  }
}
