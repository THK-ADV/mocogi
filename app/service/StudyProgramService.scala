package service

import basedata.StudyProgram
import database.repo.{StudyProgramOutput, StudyProgramRepository}
import parsing.base.StudyProgramFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait StudyProgramService {
  def all(): Future[Seq[StudyProgramOutput]]
  def create(input: String): Future[List[StudyProgram]]
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

  def create(input: String): Future[List[StudyProgram]] =
    for {
      grades <- gradeService.all()
      people <- personService.all()
      studyFormTypes <- studyFormTypeService.all()
      languages <- languageService.all()
      seasons <- seasonService.all()
      locations <- locationService.all()
      _ = println(
        grades.size,
        people.size,
        studyFormTypes.size,
        languages.size,
        seasons.size,
        locations.size
      )
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
}
