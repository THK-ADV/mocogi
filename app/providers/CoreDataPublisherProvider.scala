package providers

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.view.ModuleViewRepository
import database.view.StudyProgramViewRepository
import git.publisher.CoreDataPublisher
import git.subscriber.CoreDataPublishActor
import kafka.Topics
import models.core._
import ops.ConfigurationOps.Ops
import org.apache.pekko.actor.ActorSystem
import play.api.Configuration
import service.core._

@Singleton
final class CoreDataPublisherProvider @Inject() (
    system: ActorSystem,
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
    ctx: ExecutionContext,
    config: Configuration
) extends Provider[CoreDataPublisher] {
  override def get() = CoreDataPublisher(
    system.actorOf(
      CoreDataPublisher.props(
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
        new CoreDataPublishActor(
          system.actorOf(
            CoreDataPublishActor.props(
              config.nonEmptyString("kafka.serverUrl"),
              kafkaTopics[ModuleLocation]("moduleLocation"),
              kafkaTopics[ModuleLanguage]("moduleLanguage"),
              kafkaTopics[ModuleStatus]("moduleStatus"),
              kafkaTopics[AssessmentMethod]("assessmentMethod"),
              kafkaTopics[ModuleType]("moduleType"),
              kafkaTopics[Season]("season"),
              kafkaTopics[Identity]("identity"),
              kafkaTopics[FocusArea]("focusArea"),
              kafkaTopics[ModuleGlobalCriteria]("globalCriteria"),
              kafkaTopics[PO]("po"),
              kafkaTopics[ModuleCompetence]("competence"),
              kafkaTopics[Faculty]("faculty"),
              kafkaTopics[Degree]("degree"),
              kafkaTopics[StudyProgram]("studyProgram"),
              kafkaTopics[Specialization]("specialization"),
              ctx
            )
          )
        ),
        ctx
      )
    )
  )

  private def kafkaTopics[A](mainTopic: String): Topics[A] =
    Topics(
      config.nonEmptyString(s"kafka.topic.$mainTopic.created"),
      config.nonEmptyString(s"kafka.topic.$mainTopic.updated"),
      config.nonEmptyString(s"kafka.topic.$mainTopic.deleted")
    )
}
