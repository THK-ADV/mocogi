package controllers

import git.{GitChanges, GitConfig, ModuleCompendiumPublisher}
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
      files <- downloadFile(urls)
    } yield {
      val outputFormat = PrinterOutputFormat.DefaultPrinter
      publisher.notifyAllObservers(files, outputFormat)
      Ok(JsString("Okay!"))
    }
  }

  private def downloadFile(
      changes: GitChanges[List[String]]
  ): Future[GitChanges[List[String]]] = {
    def go(url: String): Future[String] =
      ws
        .url(url)
        .addHttpHeaders(accessTokenHeader())
        .get()
        .map(_.bodyAsBytes.utf8String)
    for {
      added <- Future.sequence(changes.added.map(go))
      modified <- Future.sequence(changes.modified.map(go))
      removed <- Future.sequence(changes.removed.map(go))
    } yield GitChanges(added, modified, removed)
  }

  private def accessTokenHeader(): (String, String) =
    "PRIVATE-TOKEN" -> gitConfig.accessToken

  private def stitchFileUrl(
      projectId: Int,
      changes: GitChanges[List[String]]
  ): GitChanges[List[String]] = {
    def urlEncoded(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8)
    def go(path: String) =
      s"${gitConfig.baseUrl}/projects/$projectId/repository/files/${urlEncoded(path)}/raw?ref=master"
    GitChanges(
      changes.added.map(go),
      changes.modified.map(go),
      Nil // TODO removed file can't be downloaded lol
    )
  }

  private def parseProjectId(implicit r: Request[JsValue]): JsResult[Int] =
    r.body.\("project_id").validate[Int]

  private def parseChanges(implicit
      r: Request[JsValue]
  ): JsResult[GitChanges[List[String]]] =
    r.body.\("commits").validate[JsArray].map { xs =>
      val last = xs.last
      GitChanges(
        last.\("added").validate[List[String]].getOrElse(Nil),
        last.\("modified").validate[List[String]].getOrElse(Nil),
        last.\("removed").validate[List[String]].getOrElse(Nil)
      )
    }

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
              BadRequest(Json.toJson(new Throwable("invalid Gitlab-Token")))
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
