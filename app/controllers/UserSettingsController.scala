package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import auth.AuthorizationAction
import play.api.mvc.AbstractController
import play.api.mvc.AnyContent
import play.api.mvc.ControllerComponents
import database.repo.UserSettingsRepository
import auth.TokenRequest
import play.api.libs.json.JsValue

@Singleton
final class UserSettingsController @Inject() (
    cc: ControllerComponents,
    auth: AuthorizationAction,
    val repo: UserSettingsRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def get() =
    auth.async { (r: TokenRequest[AnyContent]) =>
      repo.get(r.token.username).map(js => js.map(Ok(_)).getOrElse(NotFound))
    }

  def update() =
    auth(parse.json[JsValue]).async { (r: TokenRequest[JsValue]) =>
      repo.update(r.token.username, r.body).map(_ => NoContent)
    }
}
