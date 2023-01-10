package controllers

import controllers.MetadataController.{previewAttribute, userAttribute}
import controllers.json.MetadataFormat
import git.GitFilePath
import play.api.libs.json.{Format, Json, OFormat}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.{MetadataParsingValidator, MetadataService}
import validator.Metadata

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

object MetadataController {
  val userAttribute = "user"
  val previewAttribute = "preview"
}

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

  // TODO add proper querying
  def all() =
    Action.async { request =>
      val user = request.getQueryString(userAttribute)
      val preview = request
        .getQueryString(previewAttribute)
        .flatMap(_.toBooleanOption)
        .getOrElse(false)
      user match {
        case Some(user) =>
          if (preview)
            service
              .allPreviewOfUser(user)
              .map(xs => Ok(Json.toJson(xs)))
          else
            service
              .allOfUser(user)
              .map(xs => Ok(Json.toJson(xs)))
        case None =>
          if (preview)
            service
              .allPreview()
              .map(xs => Ok(Json.toJson(xs)))
          else
            service
              .all()
              .map(xs => Ok(Json.toJson(xs)))
      }
    }

  def get(id: UUID) =
    Action.async { _ =>
      service.get(id).map(x => Ok(Json.toJson(x)))
    }

  // TODO only used for testing
  def create() = textInputAction { input =>
    val path = GitFilePath("???") // TODO replace with real path
    val now = LocalDateTime.now() // TODO replace with date time
    parsingValidator
      .parse(input, path)
      .flatMap(m => service.create(m._1, path, now))
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
