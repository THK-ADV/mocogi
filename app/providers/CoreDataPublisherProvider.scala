package providers

import akka.actor.ActorSystem
import git.publisher.CoreDataPublisher
import service.core.{
  AssessmentMethodService,
  CompetenceService,
  FacultyService,
  FocusAreaService,
  GlobalCriteriaService,
  GradeService,
  LanguageService,
  LocationService,
  ModuleTypeService,
  POService,
  PersonService,
  SeasonService,
  StatusService,
  StudyFormTypeService,
  StudyProgramService
}

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
        personService,
        focusAreaService,
        globalCriteriaService,
        poService,
        competenceService,
        facultyService,
        gradeService,
        studyProgramService,
        studyFormTypeService,
        ctx
      )
    )
  )
}
