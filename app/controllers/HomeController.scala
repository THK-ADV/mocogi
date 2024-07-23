package controllers

import play.api.libs.json.Json
import play.api.mvc._
import play.api.routing.Router

import javax.inject._

@Singleton
class HomeController @Inject() (
    cc: ControllerComponents,
    router: Provider[Router]
) extends AbstractController(cc) {

  def index = Action { _ =>
    Ok(
      Json.obj(
        "msg" -> "it works",
        "routes" -> router.get().documentation.map(t => (t._1, t._2))
      )
    )
  }
}
