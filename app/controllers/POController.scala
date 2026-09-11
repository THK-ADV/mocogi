package controllers

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import controllers.POController.ids
import controllers.POController.validAttribute
import play.api.libs.json.Json
import play.api.mvc.AbstractController
import play.api.mvc.ControllerComponents
import database.repo.core.PORepository

object POController {
  val validAttribute = "valid"
  val ids            = "ids"
}

@Singleton
final class POController @Inject() (
    cc: ControllerComponents,
    repo: PORepository,
    cache: ResourceCache,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def all() =
    cache("pos", 1.hour) {
      Action.async { request =>
        val validOnly = request
          .getQueryString(validAttribute)
          .flatMap(_.toBooleanOption)
          .getOrElse(true)
        val res =
          if (validOnly) repo.allValid()
          else {
            val poIds = request.getQueryString(ids).map(_.split(',').toList)
            poIds.fold(repo.list())(repo.allWithIds)
          }
        res.map(xs => Ok(Json.toJson(xs)))
      }
    }
}
