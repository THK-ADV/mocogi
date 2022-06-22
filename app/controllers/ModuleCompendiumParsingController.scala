package controllers

import com.google.common.base.Charsets
import com.google.common.io.Files
import controllers.ModuleCompendiumParsingController.{
  mcgErrorWrites,
  moduleCompendiumFormat,
  parsingErrorWrites,
  throwableWrites
}
import parser.ParsingError
import parsing.ModuleCompendiumParser
import parsing.types.ModuleRelation.{Child, Parent}
import parsing.types.{Status => ModuleStatus, _}
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.{AbstractController, ControllerComponents, Request, Result}
import printer.PrintingError
import printing.{ModuleCompendiumGenerationError, ModuleCompendiumPrinter}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

@Singleton
class ModuleCompendiumParsingController @Inject() (
    cc: ControllerComponents,
    ws: WSClient,
    moduleCompendiumParser: ModuleCompendiumParser,
    moduleCompendiumPrinter: ModuleCompendiumPrinter,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def parseFile() = Action(parse.temporaryFile) { r =>
    extractFileContent(r).map { input =>
      moduleCompendiumParser.parser.parse(input)._1 match {
        case Right(c) => Ok(Json.toJson(c))
        case Left(e)  => InternalServerError(Json.toJson(e))
      }
    }
  }

  def generateFromFile() = Action(parse.temporaryFile) { r =>
    extractFileContent(r).map(generate0)
  }

  def generateFromUrl() = Action(parse.json).async { r =>
    r.body.\("url").validate[String].asOpt match {
      case Some(url) =>
        ws.url(url).get().map(r => generate0(r.bodyAsBytes.utf8String))
      case None =>
        Future.successful(InternalServerError(JsString("no url found")))
    }
  }

  private def generate0(input: String): Result =
    moduleCompendiumPrinter.generate(input) match {
      case Right(html) => Ok(html)
      case Left(e)     => InternalServerError(Json.toJson(e))
    }

  private def extractFileContent(r: Request[TemporaryFile]) =
    Try(Files.asCharSource(r.body.path.toFile, Charsets.UTF_8).read())

  private implicit def tryToResult(`try`: Try[Result]): Result =
    `try` match {
      case Success(value) => value
      case Failure(e)     => InternalServerError(Json.toJson(e))
    }
}

object ModuleCompendiumParsingController extends JsonNullWritable {

  implicit val mcgErrorWrites: Writes[ModuleCompendiumGenerationError] =
    Writes.apply {
      case ModuleCompendiumGenerationError.Parsing(e)  => Json.toJson(e)
      case ModuleCompendiumGenerationError.Printing(e) => Json.toJson(e)
      case ModuleCompendiumGenerationError.Other(e)    => Json.toJson(e)
    }

  implicit val throwableWrites: Writes[Throwable] =
    Writes.apply(e =>
      Json.obj(
        "type" -> "throwable",
        "message" -> e.getMessage
      )
    )

  implicit val parsingErrorWrites: Writes[ParsingError] =
    Writes.apply(e =>
      Json.obj(
        "type" -> "parsing error",
        "expected" -> e.expected,
        "actual" -> e.found
      )
    )

  implicit val printingErrorWrites: Writes[PrintingError] =
    Writes.apply(e =>
      Json.obj(
        "type" -> "printing error",
        "expected" -> e.expected,
        "actual" -> e.actual
      )
    )

  implicit val moduleTypeFormat: Format[ModuleType] =
    Json.format[ModuleType]

  implicit val languageFormat: Format[Language] =
    Json.format[Language]

  implicit val seasonFormat: Format[Season] =
    Json.format[Season]

  implicit val statusFormat: Format[ModuleStatus] =
    Json.format[ModuleStatus]

  implicit val peopleFormat: Format[People] =
    Json.format[People]

  implicit val responsibilitiesFormat: Format[Responsibilities] =
    Json.format[Responsibilities]

  implicit val assessmentMethodFormat: Format[AssessmentMethod] =
    Json.format[AssessmentMethod]

  implicit val workloadFormat: Format[Workload] =
    Json.format[Workload]

  implicit val contentFormat: Format[Content] =
    Json.format[Content]

  implicit val locationFormat: Format[Location] =
    Json.format[Location]

  implicit val parentFormat: Format[Parent] =
    Json.format[Parent]

  implicit val childFormat: Format[Child] =
    Json.format[Child]

  implicit val moduleRelationFormat: Format[ModuleRelation] =
    OFormat.apply(
      js =>
        js.\("type").validate[String].flatMap {
          case "parent" =>
            js.\("children").validate[List[String]].map(Parent.apply)
          case "child" =>
            js.\("parent").validate[String].map(Child.apply)
          case other =>
            JsError(s"expected type to be parent or child, but was $other")
        },
      {
        case Parent(children) =>
          Json.obj(
            "type" -> "parent",
            "children" -> Json.toJson(children)
          )
        case Child(parent) =>
          Json.obj(
            "type" -> "child",
            "parent" -> Json.toJson(parent)
          )
      }
    )

  implicit val assessmentMethodPercentFormat
      : Format[AssessmentMethodPercentage] =
    Json.format[AssessmentMethodPercentage]

  implicit val metaDataFormat: Format[Metadata] =
    Json.format[Metadata]

  implicit val moduleCompendiumFormat: Format[ModuleCompendium] =
    Json.format[ModuleCompendium]
}
