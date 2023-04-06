package git.publisher

import akka.actor.{Actor, ActorRef, Props}
import database.InsertOrUpdateResult
import git.GitFilesBroker.Changes
import git.publisher.CoreDataPublisher.ParsingValidation
import git.{GitFileContent, GitFilePath, GitFilesBroker}
import play.api.Logging
import service.core._

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object CoreDataPublisher {
  def props(
      locationService: LocationService,
      languageService: LanguageService,
      statusService: StatusService,
      assessmentMethodService: AssessmentMethodService,
      moduleTypeService: ModuleTypeService,
      seasonService: SeasonService,
      personService: PersonService,
      focusAreaService: FocusAreaService,
      globalCriteriaService: GlobalCriteriaService,
      poService: POService,
      competenceService: CompetenceService,
      facultyService: FacultyService,
      gradeService: GradeService,
      studyProgramService: StudyProgramService,
      studyFormTypeService: StudyFormTypeService,
      specializationService: SpecializationService,
      ctx: ExecutionContext
  ) =
    Props(
      new CoreDataPublisherImpl(
        locationService,
        languageService,
        statusService,
        assessmentMethodService,
        moduleTypeService,
        seasonService,
        personService,
        focusAreaService,
        globalCriteriaService,
        poService,
        competenceService,
        facultyService,
        gradeService,
        studyProgramService,
        studyFormTypeService,
        specializationService,
        ctx
      )
    )

  private final class CoreDataPublisherImpl(
      private val locationService: LocationService,
      private val languageService: LanguageService,
      private val statusService: StatusService,
      private val assessmentMethodService: AssessmentMethodService,
      private val moduleTypeService: ModuleTypeService,
      private val seasonService: SeasonService,
      private val personService: PersonService,
      private val focusAreaService: FocusAreaService,
      private val globalCriteriaService: GlobalCriteriaService,
      private val poService: POService,
      private val competenceService: CompetenceService,
      private val facultyService: FacultyService,
      private val gradeService: GradeService,
      private val studyProgramService: StudyProgramService,
      private val studyFormTypeService: StudyFormTypeService,
      private val specializationService: SpecializationService,
      private implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {
    override def receive = { case ParsingValidation(changes) =>
      parseAndUpdate(changes.modified)
      parseAndUpdate(changes.added)
    }

    /*TODO add support for deletion.
        if an entry doesn't exists anymore in a yaml file, it will not be deleted currently.
        instead, each entry will be either created (if new) or updated (if already exists).*/
    private def parseAndUpdate(
        changes: List[(GitFilePath, GitFileContent)]
    ): Unit = {
      def go(
          path: GitFilePath,
          content: GitFileContent
      ): Future[Seq[(InsertOrUpdateResult, _)]] =
        path.value
          .stripPrefix(s"${GitFilesBroker.core}/")
          .split('.')
          .headOption
          .map {
            case "location" =>
              locationService.createOrUpdate(content.value)
            case "lang" =>
              languageService.createOrUpdate(content.value)
            case "status" =>
              statusService.createOrUpdate(content.value)
            case "assessment" =>
              assessmentMethodService.createOrUpdate(content.value)
            case "module_type" =>
              moduleTypeService.createOrUpdate(content.value)
            case "season" =>
              seasonService.createOrUpdate(content.value)
            case "person" =>
              personService.createOrUpdate(content.value)
            case "focus_area" =>
              focusAreaService.createOrUpdate(content.value)
            case "global_criteria" =>
              globalCriteriaService.createOrUpdate(content.value)
            case "po" =>
              poService.createOrUpdate(content.value)
            case "competence" =>
              competenceService.createOrUpdate(content.value)
            case "faculty" =>
              facultyService.createOrUpdate(content.value)
            case "grade" =>
              gradeService.createOrUpdate(content.value)
            case "program" =>
              studyProgramService.createOrUpdate(content.value)
            case "study_form" =>
              studyFormTypeService.createOrUpdate(content.value)
            case "specialization" =>
              specializationService.createOrUpdate(content.value)
            case other =>
              Future.failed(new Throwable(s"unknown core data found: $other"))
          } getOrElse Future.failed(
          new Throwable(s"expected path to be filename.yaml, but was $path")
        )

      changes.foreach { case (path, content) =>
        go(path, content) onComplete {
          case Success(s) => logSuccess(path, content, s)
          case Failure(t) => logFailure(path, content, t)
        }
      }
    }

    private def logFailure(
        path: GitFilePath,
        content: GitFileContent,
        error: Throwable
    ): Unit =
      logger.error(s"""failed to create or update core data file
           |  - file path: ${path.value}
           |  - file content size: ${content.value.length}
           |  - message: ${error.getMessage}
           |  - trace: ${error.getStackTrace.mkString(
                       "\n           "
                     )}""".stripMargin)

    private def logSuccess(
        path: GitFilePath,
        content: GitFileContent,
        result: Seq[(InsertOrUpdateResult, _)]
    ): Unit =
      logger.info(s"""successfully created or updated core data file
           |  - file path: ${path.value}
           |  - file content size: ${content.value.length}
           |  - result: ${result.size}""".stripMargin)
  }

  private case class ParsingValidation(changes: Changes)
}

@Singleton
case class CoreDataPublisher(private val value: ActorRef) {
  def notifySubscribers(changes: Changes): Unit =
    value ! ParsingValidation(changes)
}
