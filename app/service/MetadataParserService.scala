package service

import basedata.FocusAreaPreview
import parsing.metadata.MetadataCompositeParser
import parsing.types.ParsedMetadata

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait MetadataParserService {
  def parse(input: String): Future[ParsedMetadata]
}

@Singleton
final class MetadataParserServiceImpl @Inject() (
    val metadataParser: MetadataCompositeParser,
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
    private implicit val ctx: ExecutionContext
) extends MetadataParserService {
  override def parse(input: String): Future[ParsedMetadata] =
    for {
      locations <- locationService.all()
      languages <- languageService.all()
      status <- statusService.all()
      assessmentMethods <- assessmentMethodService.all()
      moduleTypes <- moduleTypeService.all()
      seasons <- seasonService.all()
      persons <- personService.all()
      focusAreas <- focusAreaService.all()
      globalCriteria <- globalCriteriaService.all()
      pos <- poService.all()
      competences <- competenceService.all()
      metadata <- metadataParser
        .parser(
          locations,
          languages,
          status,
          assessmentMethods,
          moduleTypes,
          seasons,
          persons,
          focusAreas.map(f => FocusAreaPreview(f.abbrev)),
          competences,
          globalCriteria,
          pos
        )
        .parse(input)
        ._1
        .fold(Future.failed, Future.successful)
    } yield metadata
}