package controllers

import auth.AuthorizationAction
import controllers.formats.{ModuleReviewerRoleFormat, UserFormat}
import database.repo.ModuleReviewerRepository
import models.{ModuleReviewer, ModuleReviewerRole, User}
import play.api.libs.json.{Json, Reads, Writes}
import play.api.mvc.{AbstractController, ControllerComponents}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleReviewerController @Inject() (
    cc: ControllerComponents,
    repo: ModuleReviewerRepository,
    auth: AuthorizationAction,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with UserFormat
    with ModuleReviewerRoleFormat {

  case class Protocol(
      user: User,
      role: ModuleReviewerRole,
      studyProgram: String
  )

  implicit val writes: Writes[ModuleReviewer] = Json.writes

  implicit val reads: Reads[Protocol] = Json.reads

  def create() =
    auth(parse.json[Protocol]) async { r =>
      repo
        .create(
          ModuleReviewer(
            UUID.randomUUID(),
            r.body.user,
            r.body.role,
            r.body.studyProgram
          )
        )
        .map(_ => Created)
    }

  def all() =
    auth async { _ =>
      repo.all().map(xs => Ok(Json.toJson(xs)))
    }

  def delete(id: UUID) =
    auth async { r =>
      repo
        .delete(id)
        .map(_ => NoContent)
    }
}
