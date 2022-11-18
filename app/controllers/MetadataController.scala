package controllers

import controllers.json.MetadataFormat
import git.GitFilePath
import play.api.libs.json.{Format, Json, OFormat}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.{MetadataParsingValidator, MetadataService}
import validator.Metadata

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class MetadataController @Inject() (
    cc: ControllerComponents,
    service: MetadataService,
    parsingValidator: MetadataParsingValidator,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with MetadataFormat
    with TextInputAction {

  implicit val fmt: Format[(Metadata, GitFilePath)] =
    OFormat.apply(
      js => {
        for {
          m <- js.\("metadata").validate[Metadata]
          p <- js.\("gitFilePath").validate[String]
        } yield (m, GitFilePath(p))
      },
      (a: (Metadata, GitFilePath)) =>
        Json.obj(
          "metadata" -> Json.toJson(a._1),
          "gitFilePath" -> Json.toJson(a._2.value)
        )
    )

  // TODO only used for testing
  def all() =
    Action.async { _ =>
      service.all().map(xs => Ok(Json.toJson(xs)))
    }

  // TODO only used for testing
  def create() = textInputAction { input =>
    val path = GitFilePath("???") // TODO replace with real path
    parsingValidator
      .parse(input, path)
      .flatMap(m => service.create(m._1, path))
      .map(m => Ok(Json.toJson(m)))
  }

  // TODO only used for testing
  def parseAndValidate() = textInputAction { input =>
    val path = GitFilePath("???") // TODO replace with real path
    parsingValidator
      .parse(input, path)
      .map(m => Ok(Json.toJson(m._1)))
  }
}
