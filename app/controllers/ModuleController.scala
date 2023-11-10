package controllers

import auth.AuthorizationAction
import controllers.formats.{
  MetadataOutputFormat,
  ModuleFormat,
  StudyProgramAtomicFormat
}
import database.view.{
  ModuleViewRepository,
  PersonShort,
  StudyProgramModuleAssociation
}
import play.api.libs.json.{Format, Json}
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
) extends AbstractController(cc)
    with MetadataOutputFormat
    with StudyProgramAtomicFormat
    with ModuleFormat {

  implicit val psFmt: Format[PersonShort] =
    Json.format[PersonShort]

  implicit val studyProgramAssocFmt
      : Format[StudyProgramModuleAssociation[Iterable[Int]]] =
    Json.format[StudyProgramModuleAssociation[Iterable[Int]]]

  implicit val viewFmt: Format[ModuleViewRepository#Entry] =
    Json.format[ModuleViewRepository#Entry]

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
