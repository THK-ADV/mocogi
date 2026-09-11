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
import database.repo.core.*

@Singleton
private[pipeline] final class MetadataParsingService @Inject() (
    private val metadataParser: MetadataCompositeParser,
    private val locationRepo: LocationRepository,
    private val languageRepo: LanguageRepository,
    private val statusRepo: StatusRepository,
    private val assessmentMethodRepo: AssessmentMethodRepository,
    private val moduleTypeRepo: ModuleTypeRepository,
    private val seasonRepo: SeasonRepository,
    private val personRepo: IdentityRepository,
    private val poRepo: PORepository,
    private val specializationRepo: SpecializationRepository,
    private implicit val ctx: ExecutionContext
) {
  private type ParsedPrint   = (Print, ParsedMetadata, ModuleContent, ModuleContent)
  private type ParseFailure  = (Print, PipelineError)
  private type ParsingResult = Future[Either[Seq[PipelineError], Seq[ParsedPrint]]]

  private def parser = {
    val locations         = locationRepo.list()
    val languages         = languageRepo.list()
    val status            = statusRepo.list()
    val assessmentMethods = assessmentMethodRepo.all()
    val moduleTypes       = moduleTypeRepo.list()
    val seasons           = seasonRepo.list()
    val persons           = personRepo.list()
    val pos               = poRepo.list()
    val specializations   = specializationRepo.list()
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
    parseAll(prints).map { (errors, parsed) =>
      Either.cond(errors.isEmpty, parsed, errors.map(_._2))
    }

  private[pipeline] def parseAll(prints: Seq[Print]): Future[(Seq[ParseFailure], Seq[ParsedPrint])] =
    parser.map { p =>
      val (errs, parses) = prints.partitionMap { print =>
        val parseRes = p.parse(print.value)
        val res      = parseRes._1.bimap(identity, (print, _))
        val rest     = Rest(parseRes._2)
        res match {
          case Left(err) =>
            Left(print -> PipelineError.parser(err, None))
          case Right((print, parsedMetadata)) =>
            ModuleContentParser.parse(rest.value)._1 match {
              case Left(err) =>
                Left(print -> PipelineError.parser(err, Some(parsedMetadata.id)))
              case Right((de, en)) =>
                Right((print, parsedMetadata, de, en))
            }
        }
      }
      (errs, parses)
    }

  def parse(print: Print): Future[Either[ParsingError, (ParsedMetadata, ModuleContent, ModuleContent)]] =
    parser.map { p =>
      val (res, _) = p.zip(ModuleContentParser.parser).parse(print.value)
      res.map { case (m, (de, en)) => (m, de, en) }
    }
}
