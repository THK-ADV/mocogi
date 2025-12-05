package providers

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.view.ModuleViewRepository
import database.view.StudyProgramViewRepository
import git.publisher.CoreDataPublisher
import org.apache.pekko.actor.ActorSystem
import service.core.*

@Singleton
final class CoreDataPublisherProvider @Inject() (
    system: ActorSystem,
    locationService: LocationService,
    languageService: LanguageService,
    statusService: StatusService,
    moduleTypeService: ModuleTypeService,
    seasonService: SeasonService,
    identityService: IdentityService,
    poService: POService,
    degreeService: DegreeService,
    studyProgramService: StudyProgramService,
    specializationService: SpecializationService,
    studyProgramViewRepository: StudyProgramViewRepository,
    moduleViewRepository: ModuleViewRepository,
    ctx: ExecutionContext,
) extends Provider[CoreDataPublisher] {
  override def get() = CoreDataPublisher(
    system.actorOf(
      CoreDataPublisher.props(
        locationService,
        languageService,
        statusService,
        moduleTypeService,
        seasonService,
        identityService,
        poService,
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
