package service

import models.core._
import ops.EitherOps.EOps
import parser.ParsingError
import parsing.metadata.MetadataCompositeParser
import parsing.types.{Content, ParsedMetadata}
import service.core._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait MetadataParsingService {
  def parseMany(prints: Seq[(Option[UUID], Print)]): ParsingResult
  def parse(print: Print): Future[Either[ParsingError, (ParsedMetadata, Content, Content)]]
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

  private def parser() =
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
    } yield metadataParser
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

  def parseMany(prints: Seq[(Option[UUID], Print)]): ParsingResult =
    parser().map { p =>
      val (errs, parses) = prints.partitionMap { case (id, print) =>
        val parseRes = p.parse(print.value)
        val res = parseRes._1.bimap(
          (id, _),
          (print, _)
        )
        val rest = Rest(parseRes._2)
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

  override def parse(
      print: Print
  ): Future[Either[ParsingError, (ParsedMetadata, Content, Content)]] =
    parser().map { p =>
      val (res, rest) = p.parse(print.value)
      res.flatMap { parsedMetadata =>
        contentParsingService.parse(rest)._1.map { case (de, en) =>
          (parsedMetadata, de, en)
        }
      }
    }
}
