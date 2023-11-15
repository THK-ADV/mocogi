package controllers

import database.repo.ModuleCompendiumListRepository
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleCompendiumListController @Inject() (
    cc: ControllerComponents,
    repo: ModuleCompendiumListRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def allFromSemester(semester: String) =
    Action.async(_ =>
      repo.allFromSemester(semester).map(xs => Ok(Json.toJson(xs)))
    )
}
