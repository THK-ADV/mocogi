package service

import models.core._
import parser.ParsingError
import parsing.metadata.MetadataCompositeParser
import parsing.types.ParsedMetadata
import service.core._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait MetadataParsingService {
  def parse(print: Print): Future[(ParsedMetadata, Rest)]
  def parseMany(
      prints: Seq[(UUID, Print)]
  ): Future[
    Seq[(Either[(UUID, ParsingError), (Print, ParsedMetadata)], Rest)]
  ]
}

@Singleton
final class MetadataParsingServiceImpl @Inject() (
    private val metadataParser: MetadataCompositeParser,
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
) extends MetadataParsingService {
  import ops.EitherOps._

  private def parser(
      locations: Seq[Location],
      languages: Seq[Language],
      status: Seq[Status],
      assessmentMethods: Seq[AssessmentMethod],
      moduleTypes: Seq[ModuleType],
      seasons: Seq[Season],
      persons: Seq[Person],
      focusAreas: Seq[FocusArea],
      globalCriteria: Seq[GlobalCriteria],
      pos: Seq[PO],
      competences: Seq[Competence]
  ) =
    metadataParser
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

  override def parse(print: Print): Future[(ParsedMetadata, Rest)] =
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
      (res, rest) = parser(
        locations,
        languages,
        status,
        assessmentMethods,
        moduleTypes,
        seasons,
        persons,
        focusAreas,
        globalCriteria,
        pos,
        competences
      ).parse(print.value)
      metadata <- res.fold(
        Future.failed,
        m => Future.successful((m, Rest(rest)))
      )
    } yield metadata

  override def parseMany(
      prints: Seq[(UUID, Print)]
  ): Future[
    Seq[(Either[(UUID, ParsingError), (Print, ParsedMetadata)], Rest)]
  ] =
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
    } yield {
      val p = parser(
        locations,
        languages,
        status,
        assessmentMethods,
        moduleTypes,
        seasons,
        persons,
        focusAreas,
        globalCriteria,
        pos,
        competences
      )
      prints.map { case (id, print) =>
        val res = p.parse(print.value)
        (
          res._1.bimap(
            (id, _),
            (print, _)
          ),
          Rest(res._2)
        )
      }
    }
}
