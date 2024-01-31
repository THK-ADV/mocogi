package controllers

import auth.AuthorizationAction
import database.view.{
  ModuleManagement,
  ModuleViewRepository,
  StudyProgramModuleAssociation
}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.ModuleCompendiumService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleController @Inject() (
    cc: ControllerComponents,
    service: ModuleCompendiumService,
    moduleViewRepository: ModuleViewRepository,
    val auth: AuthorizationAction,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc) {

  implicit def moduleManagementWrites: Writes[ModuleManagement] = Json.writes

  implicit def studyProgramAssocWrites
      : Writes[StudyProgramModuleAssociation[Iterable[Int]]] =
    Json.writes

  implicit def moduleViewWrites: Writes[ModuleViewRepository#Entry] =
    Json.writes[ModuleViewRepository#Entry]

  def allModules() =
    Action.async { request =>
      service
        .allModules(request.queryString)
        .map(xs => Ok(Json.toJson(xs)))
    }

  def allModuleMetadata() =
    Action.async { request =>
      service
        .allMetadata(request.queryString)
        .map(xs => Ok(Json.toJson(xs)))
    }

  def allModulesFromView() =
    Action.async { _ =>
      moduleViewRepository
        .all()
        .map(xs => Ok(Json.toJson(xs)))
    }
}
