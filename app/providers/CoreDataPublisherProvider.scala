package providers

import akka.actor.ActorSystem
import database.view.{ModuleViewRepository, StudyProgramViewRepository}
import git.publisher.CoreDataPublisher
import service.core._

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

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
    ctx: ExecutionContext
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
        ctx
      )
    )
  )
}
