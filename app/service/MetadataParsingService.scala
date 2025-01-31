package service

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.syntax.either.*
import models.core.*
import parser.ParsingError
import parsing.metadata.MetadataCompositeParser
import parsing.types.ModuleContent
import parsing.types.ParsedMetadata
import play.api.Logging
import service.core.*

@Singleton
final class MetadataParsingService @Inject() (
    private val metadataParser: MetadataCompositeParser,
    private val locationService: LocationService,
    private val languageService: LanguageService,
    private val statusService: StatusService,
    private val assessmentMethodService: AssessmentMethodService,
    private val moduleTypeService: ModuleTypeService,
    private val seasonService: SeasonService,
    private val personService: IdentityService,
    private val focusAreaService: FocusAreaService,
    private val globalCriteriaService: GlobalCriteriaService,
    private val poService: POService,
    private val competenceService: CompetenceService,
    private val specializationService: SpecializationService,
    private implicit val ctx: ExecutionContext
) extends Logging {

  private def parser = {
    val locations         = locationService.all()
    val languages         = languageService.all()
    val status            = statusService.all()
    val assessmentMethods = assessmentMethodService.all()
    val moduleTypes       = moduleTypeService.all()
    val seasons           = seasonService.all()
    val persons           = personService.all()
    val focusAreas        = focusAreaService.all().map(_.map(f => FocusAreaID(f.id)))
    val globalCriteria    = globalCriteriaService.all()
    val pos               = poService.all()
    val competences       = competenceService.all()
    val specializations   = specializationService.all()
    for {
      locations         <- locations
      languages         <- languages
      status            <- status
      assessmentMethods <- assessmentMethods
      moduleTypes       <- moduleTypes
      seasons           <- seasons
      persons           <- persons
      focusAreas        <- focusAreas
      globalCriteria    <- globalCriteria
      pos               <- pos
      competences       <- competences
      specializations   <- specializations
    } yield metadataParser
      .parser(
        locations,
        languages,
        status,
        assessmentMethods,
        moduleTypes,
        seasons,
        persons,
        focusAreas,
        competences,
        globalCriteria,
        pos,
        specializations
      )
  }

  def parseMany(prints: Seq[Print]): ParsingResult =
    parser.map { p =>
      val (errs, parses) = prints.partitionMap { print =>
        val parseRes = p.parse(print.value)
        val res      = parseRes._1.bimap(identity, (print, _))
        val rest     = Rest(parseRes._2)
        res match {
          case Left(err) =>
            Left(PipelineError.Parser(err, None))
          case Right((print, parsedMetadata)) =>
            ContentParsingService.parse(rest.value)._1 match {
              case Left(err) =>
                Left(PipelineError.Parser(err, Some(parsedMetadata.id)))
              case Right((de, en)) =>
                Right((print, parsedMetadata, de, en))
            }
        }
      }
      Either.cond(errs.isEmpty, parses, errs)
    }

  def parse(
      print: Print
  ): Future[
    Either[ParsingError, (ParsedMetadata, ModuleContent, ModuleContent)]
  ] =
    parser.map { p =>
      val (res, rest) = p.parse(print.value)
      res.flatMap { parsedMetadata =>
        ContentParsingService.parse(rest)._1.map {
          case (de, en) =>
            (parsedMetadata, de, en)
        }
      }
    }
}
