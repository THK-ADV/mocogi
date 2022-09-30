package controllers

import controllers.json.MetadataFormat
import git.GitFilePath
import parsing.types.ParsedMetadata
import play.api.libs.json.{Format, Json, OFormat}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.MetadataService
import validator.Metadata

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class MetadataController @Inject() (
    cc: ControllerComponents,
    service: MetadataService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with MetadataFormat {

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

  def all() =
    Action.async { _ =>
      Future.successful(Seq.empty[Metadata]).map(xs => Ok(Json.toJson(xs)))
    }
}
