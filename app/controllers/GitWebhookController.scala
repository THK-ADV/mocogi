package controllers

import controllers.formats.ThrowableWrites
import git._
import git.publisher.GitFilesDownloadActor
import play.api.Logging
import play.api.libs.json._
import play.api.mvc._

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

@Singleton
class GitWebhookController @Inject() (
    cc: ControllerComponents,
    gitConfig: GitConfig,
    downloadActor: GitFilesDownloadActor
) extends AbstractController(cc)
    with Logging
    with ThrowableWrites {

  def onPushEvent() = isAuthenticated(
    Action(parse.json) { implicit r =>
      val res = for {
        projectId <- parseProjectId
        changes <- parseChanges
      } yield {
        downloadActor.download(changes, projectId)
        "Okay!"
      }

      res.fold(e => BadRequest(JsError.toJson(e)), s => Ok(JsString(s)))
    }
  )

  private def parseProjectId(implicit r: Request[JsValue]): JsResult[Int] =
    r.body.\("project_id").validate[Int]

  private def parseChanges(implicit
      r: Request[JsValue]
  ): JsResult[GitChanges[List[GitFilePath]]] =
    for {
      commits <- r.body.\("commits").validate[JsArray]
      last = commits.last
      added <- last.\("added").validate[List[String]]
      modified <- last.\("modified").validate[List[String]]
      removed <- last.\("removed").validate[List[String]]
      commitId <- last.\("id").validate[String]
      timestamp <- last.\("timestamp").validate[LocalDateTime]
    } yield GitChanges(
      added.map(GitFilePath.apply),
      modified.map(GitFilePath.apply),
      removed.map(GitFilePath.apply),
      commitId,
      timestamp
    )

  private def isAuthenticated[A](action: Action[A]) = {
    def parseGitToken(implicit r: Request[_]): Try[UUID] =
      r.headers.get("X-Gitlab-Token") match {
        case Some(s) => Try(UUID.fromString(s))
        case None    => Failure(new Throwable("expected X-Gitlab-Token header"))
      }

    Action.async(action.parser) { r =>
      parseGitToken(r) match {
        case Success(t) =>
          if (gitConfig.gitToken.fold(true)(_ == t)) action(r)
          else
            Future.successful(
              Unauthorized(Json.toJson(new Throwable("invalid Gitlab-Token")))
            )
        case Failure(e) =>
          Future.successful(BadRequest(Json.toJson(e)))
      }
    }
  }
}
