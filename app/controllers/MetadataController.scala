package controllers

import controllers.json.MetadataFormat
import git.GitFilePath
import play.api.libs.json.{Format, JsTrue, Json, OFormat}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.{MetadataPipeline, MetadataService}
import validator.Metadata

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class MetadataController @Inject() (
    cc: ControllerComponents,
    service: MetadataService,
    pipeline: MetadataPipeline,
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
    pipeline.go(input, GitFilePath("???")).map(m => Ok(Json.toJson(m)))
  }
}
