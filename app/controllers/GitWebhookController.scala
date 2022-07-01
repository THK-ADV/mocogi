package controllers

import controllers.GitWebhookController.gitChangesStringFmt
import git.{GitChanges, GitToken}
import play.api.Logging
import play.api.libs.json._
import play.api.mvc._

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

@Singleton
class GitWebhookController @Inject() (
    cc: ControllerComponents,
    gitToken: GitToken
) extends AbstractController(cc)
    with Logging
    with ThrowableWrites {

  def onPushEvent() = isAuthenticated(
    Action(parse.json) { implicit r =>
      for {
        webUrl <- parseWebUrl
        changes <- parseChanges
      } yield Json.toJson(changes.map(stitchFileUrl(webUrl)))
    }
  )

  private def stitchFileUrl(base: String)(path: String): String =
    s"$base/-/raw/master/$path"

  private def parseWebUrl(implicit r: Request[JsValue]): JsResult[String] =
    r.body.\("project").\("web_url").validate[String]

  private def parseChanges(implicit
      r: Request[JsValue]
  ): JsResult[GitChanges[String]] =
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
          if (gitToken.value.fold(true)(_ == t)) action(r)
          else
            Future.successful(
              BadRequest(Json.toJson(new Throwable("invalid Gitlab-Token")))
            )
        case Failure(e) =>
          Future.successful(BadRequest(Json.toJson(e)))
      }
    }
  }

  private implicit def jsResultToResult(jsResult: JsResult[JsValue]): Result =
    jsResult match {
      case JsSuccess(value, _) => Ok(value)
      case JsError(errors)     => BadRequest(JsError.toJson(errors))
    }
}

object GitWebhookController {
  implicit val gitChangesStringFmt: Format[GitChanges[String]] =
    Json.format[GitChanges[String]]
}
