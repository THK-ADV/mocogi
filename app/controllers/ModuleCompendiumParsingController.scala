package controllers

import com.google.common.base.Charsets
import com.google.common.io.Files
import controllers.ModuleCompendiumParsingController.{moduleCompendiumFormat, parsingErrorFormat}
import parser.ParsingError
import parsing.ModuleCompendiumParser.moduleCompendiumParser
import parsing.types.ModuleRelation.{Child, Parent}
import parsing.types.{Status => ModuleStatus, _}
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{Format, JsError, Json, OFormat}
import play.api.mvc.{AbstractController, ControllerComponents, Request, Result}
import printing.ModuleCompendiumPrinter

import javax.inject.{Inject, Singleton}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

@Singleton
class ModuleCompendiumParsingController @Inject() (
    cc: ControllerComponents
) extends AbstractController(cc) {

  def parseFile() = Action(parse.temporaryFile) { r =>
    extractFileContent(r).map { input =>
      val (res, rest) = moduleCompendiumParser.parse(input)
      if (rest.nonEmpty)
        failure(s"remaining input should be fully consumed, but was $rest")
      else
        res match {
          case Right(c) => success(Json.toJson(c))
          case Left(e)  => failure(Json.toJson(e))
        }
    }
  }

  def generate() = Action(parse.temporaryFile) { r =>
    extractFileContent(r).map { input =>
      ModuleCompendiumPrinter.generate(input) match {
        case Right(html) => Ok(html)
        case Left(e) => InternalServerError(Json.obj("error" -> e.getMessage))
      }
    }
  }

  private def extractFileContent(r: Request[TemporaryFile]) =
    Try(Files.asCharSource(r.body.path.toFile, Charsets.UTF_8).read())

  private def failure(e: Json.JsValueWrapper) =
    InternalServerError(Json.obj("parsing-error" -> e))

  private def success(e: Json.JsValueWrapper) =
    Ok(Json.obj("module-compendium" -> e))

  private implicit def tryToResult(`try`: Try[Result]): Result =
    `try` match {
      case Success(value) => value
      case Failure(e) => InternalServerError(Json.obj("error" -> e.getMessage))
    }
}

object ModuleCompendiumParsingController extends JsonNullWritable {
  implicit val parsingErrorFormat: Format[ParsingError] =
    Json.format[ParsingError]

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

  implicit val metaDataFormat: Format[Metadata] =
    Json.format[Metadata]

  implicit val moduleCompendiumFormat: Format[ModuleCompendium] =
    Json.format[ModuleCompendium]
}
