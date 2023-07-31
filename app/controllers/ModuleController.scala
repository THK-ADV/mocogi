package controllers

import controllers.formats.{MetadataOutputFormat, StudyProgramAtomicFormat}
import database.view.{
  ModuleViewRepository,
  PersonShort,
  StudyProgramModuleAssociation
}
import play.api.libs.json.{Format, Json}
import play.api.mvc.{AbstractController, ControllerComponents}
import service.{Module, ModuleCompendiumService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class ModuleController @Inject() (
    cc: ControllerComponents,
    service: ModuleCompendiumService,
    moduleViewRepository: ModuleViewRepository,
    implicit val ctx: ExecutionContext
) extends AbstractController(cc)
    with MetadataOutputFormat
    with StudyProgramAtomicFormat {

  implicit val moduleFormat: Format[Module] =
    Json.format[Module]

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
