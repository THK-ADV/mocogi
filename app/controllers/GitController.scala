package controllers

import database.table.UserBranch
import play.api.libs.json.{Format, Json, Reads, Writes}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.GitService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class GitController @Inject() (
    cc: ControllerComponents,
    val service: GitService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  private implicit val usernameReads: Reads[String] =
    Reads.apply(_.\("username").validate(Reads.StringReads))

  private implicit val userBranchFmt: Format[UserBranch] =
    Json.format[UserBranch]

  def createBranch() =
    Action.async(parse.json(usernameReads)) { r =>
      service.createBranch(r.body).map(a => Created(Json.toJson(a)))
    }

  def branchForUser(username: String) =
    Action.async { _ =>
      service.branchForUser(username).map(a => Ok(Json.toJson(a)))
    }

  def deleteBranch(username: String) =
    Action.async { _ =>
      service.deleteBranch(username).map(_ => NoContent)
    }
}
