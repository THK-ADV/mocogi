package controllers

import controllers.GitWebhookController.{
  GitlabTokenHeader,
  ModuleModeTokenHeader
}
import controllers.formats.ThrowableWrites
import git._
import git.publisher.GitFilesDownloadActor
import git.webhook.{GitMergeEventHandlingActor, GitPushEventHandler}
import play.api.Logging
import play.api.libs.json._
import play.api.mvc._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object GitWebhookController {
  val GitlabTokenHeader = "X-Gitlab-Token"
  val ModuleModeTokenHeader = "Mocogi-Module-Mode-Token-Header"
}

@Singleton
class GitWebhookController @Inject() (
    cc: ControllerComponents,
    gitConfig: GitConfig,
    gitMergeEventHandlingActor: GitMergeEventHandlingActor,
    downloadActor: GitFilesDownloadActor
) extends AbstractController(cc)
    with Logging
    with ThrowableWrites {

  def onPushEvent() = isAuthenticated(
    Action(parse.json) { implicit r =>
      GitPushEventHandler.handlePushEvent(
        downloadActor,
        moduleMode,
        gitConfig.modulesRootFolder
      )
      NoContent
    }
  )

  def onMergeEvent() = isAuthenticated(
    Action(parse.json) { implicit r =>
      gitMergeEventHandlingActor.handle(r.body)
      NoContent
    }
  )

  private def moduleMode(implicit r: Request[_]): Boolean = {
    val moduleMode = for {
      moduleModeToken <- gitConfig.moduleModeToken
      headerToken <- r.headers
        .get(ModuleModeTokenHeader)
        .flatMap(s => Try(UUID.fromString(s)).toOption)
    } yield moduleModeToken == headerToken
    moduleMode getOrElse false
  }

  private def isAuthenticated[A](action: Action[A]) = {
    def parseGitToken(implicit r: Request[_]): Try[UUID] =
      r.headers.get(GitlabTokenHeader) match {
        case Some(s) => Try(UUID.fromString(s))
        case None =>
          Failure(new Throwable(s"expected $GitlabTokenHeader header"))
      }

    Action.async(action.parser) { r =>
      parseGitToken(r) match {
        case Success(t) =>
          if (gitConfig.gitToken.fold(true)(_ == t)) action(r)
          else
            Future.successful(
              Unauthorized(
                Json.toJson(new Throwable(s"invalid $GitlabTokenHeader"))
              )
            )
        case Failure(e) =>
          Future.successful(BadRequest(Json.toJson(e)))
      }
    }
  }
}
