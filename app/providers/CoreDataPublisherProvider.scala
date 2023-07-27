package providers

import akka.actor.ActorSystem
import database.view.{MetadataViewRepository, StudyProgramViewRepository}
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
    studyProgramViewRepository: StudyProgramViewRepository,
    metadataViewRepository: MetadataViewRepository,
    ctx: ExecutionContext,
    configReader: ConfigReader
) extends Provider[CoreDataPublisher] {
  override def get() = CoreDataPublisher(
    system.actorOf(
      CoreDataPublisher.props(
        configReader.coreRootFolder,
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
        studyProgramViewRepository,
        metadataViewRepository,
        ctx
      )
    )
  )
}
