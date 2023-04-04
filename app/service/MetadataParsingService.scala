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
  def parse(prints: Seq[(Option[UUID], Print)]): ParsingResult
}

@Singleton
final class MetadataParsingServiceImpl @Inject() (
    private val metadataParser: MetadataCompositeParser,
    private val contentParsingService: ContentParsingService,
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
    private val specializationService: SpecializationService,
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
      competences: Seq[Competence],
      specializations: Seq[Specialization]
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
        pos,
        specializations
      )

  private def parseMany(prints: Seq[(Option[UUID], Print)]): Future[
    Seq[(Either[(Option[UUID], ParsingError), (Print, ParsedMetadata)], Rest)]
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
      specializations <- specializationService.all()
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
        competences,
        specializations
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

  def parse(prints: Seq[(Option[UUID], Print)]): ParsingResult =
    parseMany(prints).map { res =>
      val (errs, parses) = res.partitionMap { case (res, rest) =>
        res match {
          case Left((id, err)) => Left(PipelineError.Parser(err, id))
          case Right((print, parsedMetadata)) =>
            contentParsingService.parse(rest.value)._1 match {
              case Left(err) =>
                Left(PipelineError.Parser(err, Some(parsedMetadata.id)))
              case Right((de, en)) => Right((print, parsedMetadata, de, en))
            }
        }
      }
      Either.cond(errs.isEmpty, parses, errs)
    }
}
