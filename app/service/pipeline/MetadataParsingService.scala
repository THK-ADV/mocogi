package service.pipeline

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.syntax.either.*
import parser.ParsingError
import parsing.content.ModuleContentParser
import parsing.metadata.MetadataCompositeParser
import parsing.types.ModuleContent
import parsing.types.ParsedMetadata
import parsing.types.Rest
import play.api.Logging
import service.core.*
import service.AssessmentMethodService
import service.ParsingResult

@Singleton
private[pipeline] final class MetadataParsingService @Inject() (
    private val metadataParser: MetadataCompositeParser,
    private val locationService: LocationService,
    private val languageService: LanguageService,
    private val statusService: StatusService,
    private val assessmentMethodService: AssessmentMethodService,
    private val moduleTypeService: ModuleTypeService,
    private val seasonService: SeasonService,
    private val personService: IdentityService,
    private val poService: POService,
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
    val pos               = poService.all()
    val specializations   = specializationService.all()
    for {
      locations         <- locations
      languages         <- languages
      status            <- status
      assessmentMethods <- assessmentMethods
      moduleTypes       <- moduleTypes
      seasons           <- seasons
      persons           <- persons
      pos               <- pos
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
            ModuleContentParser.parse(rest.value)._1 match {
              case Left(err) =>
                Left(PipelineError.Parser(err, Some(parsedMetadata.id)))
              case Right((de, en)) =>
                Right((print, parsedMetadata, de, en))
            }
        }
      }
      Either.cond(errs.isEmpty, parses, errs)
    }

  def parse(print: Print): Future[Either[ParsingError, (ParsedMetadata, ModuleContent, ModuleContent)]] =
    parser.map { p =>
      val (res, rest) = p.zip(ModuleContentParser.parser).parse(print.value)
      if rest.nonEmpty then {
        logger.error(
          s"failed to parse ${print.value.take(20)} …. expected file to be fully parsed, but the remaining string is: $rest"
        )
      }
      res.map { case (m, (de, en)) => (m, de, en) }
    }
}
