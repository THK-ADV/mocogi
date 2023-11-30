package controllers

import database.repo.ModuleCompendiumListRepository
import models.Semester
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import service.ModuleCompendiumLatexActor

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleCompendiumListController @Inject() (
    cc: ControllerComponents,
    repo: ModuleCompendiumListRepository,
    moduleCompendiumLatexActor: ModuleCompendiumLatexActor,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  def allFromSemester(semester: String) =
    Action.async(_ =>
      repo.allFromSemester(semester).map(xs => Ok(Json.toJson(xs)))
    )

  def generate(semester: String) =
    Action { _ =>
      moduleCompendiumLatexActor.generateLatexFiles(Semester(semester))
      NoContent
    }
}
