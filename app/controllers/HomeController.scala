package controllers

import javax.inject._

import play.api.libs.json.Json
import play.api.mvc._
import play.api.routing.Router

@Singleton
class HomeController @Inject() (
    cc: ControllerComponents,
    router: Provider[Router]
) extends AbstractController(cc) {

  def index = Action { (_: Request[AnyContent]) =>
    Ok(
      Json.obj(
        "msg"    -> "it works",
        "routes" -> router.get().documentation.map(t => (t._1, t._2))
      )
    )
  }
}
