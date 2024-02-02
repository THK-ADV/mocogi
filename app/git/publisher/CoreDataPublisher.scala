package git.publisher

import akka.actor.{Actor, ActorRef, Props}
import database.view.{ModuleViewRepository, StudyProgramViewRepository}
import git.GitFilesBroker.Changes
import git.publisher.CoreDataPublisher.ParsingValidation
import git.{GitFileContent, GitFilePath}
import play.api.Logging
import service.core._

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object CoreDataPublisher {
  def props(
      folderPrefix: String,
      locationService: LocationService,
      languageService: LanguageService,
      statusService: StatusService,
      assessmentMethodService: AssessmentMethodService,
      moduleTypeService: ModuleTypeService,
      seasonService: SeasonService,
      identityService: IdentityService,
      focusAreaService: FocusAreaService,
      globalCriteriaService: GlobalCriteriaService,
      poService: POService,
      competenceService: CompetenceService,
      facultyService: FacultyService,
      degreeService: DegreeService,
      studyProgramService: StudyProgramService,
      specializationService: SpecializationService,
      studyProgramViewRepository: StudyProgramViewRepository,
      moduleViewRepository: ModuleViewRepository,
      ctx: ExecutionContext
  ) =
    Props(
      new CoreDataPublisherImpl(
        folderPrefix,
        locationService,
        languageService,
        statusService,
        assessmentMethodService,
        moduleTypeService,
        seasonService,
        identityService,
        focusAreaService,
        globalCriteriaService,
        poService,
        competenceService,
        facultyService,
        degreeService,
        studyProgramService,
        specializationService,
        studyProgramViewRepository,
        moduleViewRepository,
        ctx
      )
    )

  private final class CoreDataPublisherImpl(
      private val folderPrefix: String,
      private val locationService: LocationService,
      private val languageService: LanguageService,
      private val statusService: StatusService,
      private val assessmentMethodService: AssessmentMethodService,
      private val moduleTypeService: ModuleTypeService,
      private val seasonService: SeasonService,
      private val identityService: IdentityService,
      private val focusAreaService: FocusAreaService,
      private val globalCriteriaService: GlobalCriteriaService,
      private val poService: POService,
      private val competenceService: CompetenceService,
      private val facultyService: FacultyService,
      private val degreeService: DegreeService,
      private val studyProgramService: StudyProgramService,
      private val specializationService: SpecializationService,
      private val studyProgramViewRepository: StudyProgramViewRepository,
      private val moduleViewRepository: ModuleViewRepository,
      private implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {
    override def receive = { case ParsingValidation(changes) =>
      val res = for {
        res <- Future.sequence(
          (changes.modified ::: changes.added).map(a =>
            createOrUpdate(a._1, a._2)
          )
        )
        _ <- studyProgramViewRepository.refreshView()
        _ <- moduleViewRepository.refreshView()
      } yield res

      res onComplete {
        case Success(xs) =>
          xs.foreach {
            case (path, Some(count)) => logSuccess(path, count)
            case (path, None)        => logUnknownFile(path)
          }
        case Failure(t) => logFailure(t)
      }
    }

    /*TODO add support for deletion.
            if an entry doesn't exists anymore in a yaml file, it will not be deleted currently.
            instead, each entry will be either created (if new) or updated (if already exists).*/
    private def createOrUpdate(
        path: GitFilePath,
        content: GitFileContent
    ): Future[(GitFilePath, Option[Int])] =
      path.value
        .stripPrefix(s"$folderPrefix/")
        .split('.')
        .headOption match {
        case Some(value) =>
          val res: Future[Option[Int]] = value match {
            case "location" =>
              locationService
                .createOrUpdate(content.value)
                .map(a => Some(a.size))
            case "lang" =>
              languageService
                .createOrUpdate(content.value)
                .map(a => Some(a.size))
            case "status" =>
              statusService.createOrUpdate(content.value).map(a => Some(a.size))
            case "assessment" =>
              assessmentMethodService
                .createOrUpdate(content.value)
                .map(a => Some(a.size))
            case "module_type" =>
              moduleTypeService
                .createOrUpdate(content.value)
                .map(a => Some(a.size))
            case "season" =>
              seasonService.createOrUpdate(content.value).map(a => Some(a.size))
            case "person" =>
              identityService
                .createOrUpdate(content.value)
                .map(a => Some(a.size))
            case "focus_area" =>
              focusAreaService
                .createOrUpdate(content.value)
                .map(a => Some(a.size))
            case "global_criteria" =>
              globalCriteriaService
                .createOrUpdate(content.value)
                .map(a => Some(a.size))
            case "po" =>
              poService.createOrUpdate(content.value).map(a => Some(a.size))
            case "competence" =>
              competenceService
                .createOrUpdate(content.value)
                .map(a => Some(a.size))
            case "faculty" =>
              facultyService
                .createOrUpdate(content.value)
                .map(a => Some(a.size))
            case "grade" =>
              degreeService.createOrUpdate(content.value).map(a => Some(a.size))
            case "program" =>
              studyProgramService
                .createOrUpdate(content.value)
                .map(a => Some(a.size))
            case "specialization" =>
              specializationService
                .createOrUpdate(content.value)
                .map(a => Some(a.size))
            case _ =>
              Future.successful(None)
          }
          res.map(path -> _)
        case None =>
          Future.failed(
            new Throwable(s"expected path to be filename.yaml, but was $path")
          )
      }

    private def logFailure(error: Throwable): Unit =
      logger.error(s"""failed to create or update core data file
           |  - message: ${error.getMessage}
           |  - trace: ${error.getStackTrace.mkString(
                       "\n           "
                     )}""".stripMargin)

    private def logUnknownFile(path: GitFilePath): Unit =
      logger.info(s"no handler found for ${path.value}")

    private def logSuccess(path: GitFilePath, size: Int): Unit =
      logger.info(s"""successfully created or updated core data file
           |  - file path: ${path.value}
           |  - result: $size""".stripMargin)
  }

  private case class ParsingValidation(changes: Changes)
}

@Singleton
case class CoreDataPublisher(private val value: ActorRef) {
  def notifySubscribers(changes: Changes): Unit =
    value ! ParsingValidation(changes)
}
