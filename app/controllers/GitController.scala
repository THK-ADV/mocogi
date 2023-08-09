package controllers

import auth.AuthorizationAction
import controllers.formats.UserBranchFormat
import git.api.GitBranchService
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class GitController @Inject() (
    cc: ControllerComponents,
    val service: GitBranchService,
    val auth: AuthorizationAction,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with UserBranchFormat {

  def createBranch() =
    auth.async { r =>
      service.createBranch(r.token.username).map(a => Created(Json.toJson(a)))
    }

  def branchForUser() =
    auth.async { r =>
      service.branchForUser(r.token.username).map(a => Ok(Json.toJson(a)))
    }

  def deleteBranch() =
    auth.async { r =>
      service.deleteBranch(r.token.username).map(_ => NoContent)
    }
}
