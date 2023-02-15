package controllers

import controllers.formats.UserBranchFormat
import git.api.GitBranchService
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class GitController @Inject() (
    cc: ControllerComponents,
    val service: GitBranchService,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with UserBranchFormat {

  private implicit val usernameReads: Reads[String] =
    Reads.apply(_.\("username").validate(Reads.StringReads))

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
