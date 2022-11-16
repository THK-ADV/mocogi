package providers

import akka.actor.ActorSystem
import git.publisher.CoreDataParsingValidator
import service._

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class CoreDataParsingValidatorProvider @Inject() (
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
) extends Provider[CoreDataParsingValidator] {
  override def get() = CoreDataParsingValidator(
    system.actorOf(
      CoreDataParsingValidator.props(
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
