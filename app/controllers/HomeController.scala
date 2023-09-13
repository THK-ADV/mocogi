package controllers

import auth.AuthorizationAction
import play.api.libs.json.Json
import play.api.mvc._
import play.api.routing.Router

import javax.inject._

@Singleton
class HomeController @Inject() (
    cc: ControllerComponents,
    router: Provider[Router],
    auth: AuthorizationAction
) extends AbstractController(cc) {

  def index = auth { r =>
    Ok(
      Json.obj(
        "msg" -> "it works",
        "routes" -> router.get().documentation.map(t => (t._1, t._2)),
        "token" -> Json.obj(
          "firstname" -> r.token.firstname,
          "lastname" -> r.token.lastname,
          "username" -> r.token.username,
          "email" -> r.token.email,
          "roles" -> r.token.roles
        )
      )
    )
  }
}
