package controllers

import controllers.json.MetadataFormat
import git.GitFilePath
import parsing.types.Metadata
import play.api.libs.json.{Format, Json, OFormat}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.MetadataService
import validator.ValidMetadata

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class MetadataController @Inject() (
    cc: ControllerComponents,
    service: MetadataService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with MetadataFormat {

  implicit val fmt: Format[(ValidMetadata, GitFilePath)] =
    OFormat.apply(
      js => {
        for {
          m <- js.\("metadata").validate[ValidMetadata]
          p <- js.\("gitFilePath").validate[String]
        } yield (m, GitFilePath(p))
      },
      (a: (ValidMetadata, GitFilePath)) =>
        Json.obj(
          "metadata" -> Json.toJson(a._1),
          "gitFilePath" -> Json.toJson(a._2.value)
        )
    )

  def all() =
    Action.async { _ =>
      Future.successful(Seq.empty[ValidMetadata]).map(xs => Ok(Json.toJson(xs)))
    }
}
