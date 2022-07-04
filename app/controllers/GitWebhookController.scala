package controllers

import controllers.json.ThrowableWrites
import controllers.parameter.PrinterOutputFormat
import git._
import git.publisher.ModuleCompendiumPublisher
import play.api.Logging
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

@Singleton
class GitWebhookController @Inject() (
    cc: ControllerComponents,
    gitConfig: GitConfig,
    publisher: ModuleCompendiumPublisher,
    ws: WSClient,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with Logging
    with ThrowableWrites {

  def onPushEvent() = Action(parse.json).async { implicit r =>
    for {
      projectId <- Future.fromTry(asTry(parseProjectId))
      changes <- Future.fromTry(asTry(parseChanges))
      urls = stitchFileUrl(projectId, changes)
      fileContent <- downloadFile(urls)
    } yield {
      val outputFormat = PrinterOutputFormat.DefaultPrinter
      publisher.notifySubscribers(fileContent, outputFormat)
      Ok(JsString("Okay!"))
    }
  }

  private def downloadFile(
      changes: GitChanges[List[(GitFilePath, GitFileURL)]]
  ): Future[GitChanges[List[(GitFilePath, GitFileContent)]]] = {
    def go(
        t: (GitFilePath, GitFileURL)
    ): Future[(GitFilePath, GitFileContent)] =
      ws
        .url(t._2.value)
        .addHttpHeaders(accessTokenHeader())
        .get()
        .map(r => t._1 -> GitFileContent(r.bodyAsBytes.utf8String))
    for {
      added <- Future.sequence(changes.added.map(go))
      modified <- Future.sequence(changes.modified.map(go))
      removed <- Future.sequence(changes.removed.map(go))
    } yield changes.copy(added, modified, removed)
  }

  private def accessTokenHeader(): (String, String) =
    "PRIVATE-TOKEN" -> gitConfig.accessToken

  private def stitchFileUrl(
      projectId: Int,
      changes: GitChanges[List[GitFilePath]]
  ): GitChanges[List[(GitFilePath, GitFileURL)]] = {
    def urlEncoded(path: GitFilePath) =
      URLEncoder.encode(path.value, StandardCharsets.UTF_8)
    def go(path: GitFilePath): GitFileURL =
      GitFileURL(
        s"${gitConfig.baseUrl}/projects/$projectId/repository/files/${urlEncoded(path)}/raw?ref=master"
      )
    changes.copy(
      changes.added.map(p => p -> go(p)),
      changes.modified.map(p => p -> go(p)),
      Nil // TODO removed file can't be downloaded lol. find a better way. maybe a previous version?
    )
  }

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
    } yield GitChanges(
      added.map(GitFilePath.apply),
      modified.map(GitFilePath.apply),
      removed.map(GitFilePath.apply),
      commitId
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

  private def asTry[A](jsResult: JsResult[A]): Try[A] =
    JsResult.toTry(
      jsResult,
      errors =>
        new Throwable(errors.errors.foldLeft("") { case (acc, err) =>
          acc + err._2.mkString(", ")
        })
    )
}
