package service

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.ModuleRepository
import models.core.Specialization
import models.MetadataProtocol
import models.ModuleCore
import models.ModulePOMandatoryProtocol
import models.ModuleProtocol
import ops.single
import parsing.types.Module
import play.api.libs.json.*

@Singleton
final class ModuleService @Inject() (
    private val repo: ModuleRepository,
    private val moduleCreationService: ModuleCreationService,
    private val moduleCompanionService: ModuleCompanionService,
    private implicit val ctx: ExecutionContext
) {

  def createOrUpdateMany(modules: Seq[(Module, LocalDateTime)]): Future[Unit] =
    repo.createOrUpdateMany(modules)

  def get(id: UUID): Future[ModuleProtocol] =
    repo.all(Map("id" -> Seq(id.toString))).single.map(_._1)

  def getLecturers(id: UUID): Future[Seq[String]] =
    repo.getLecturers(id)

  def getPOs(id: UUID): Future[JsValue] = {
    def toJson(m: ModulePOMandatoryProtocol, isMandatory: Boolean) =
      Json.obj(
        "po"                  -> m.po,
        "specialization"      -> m.specialization.fold(JsNull)(JsString.apply),
        "recommendedSemester" -> m.recommendedSemester,
        "mandatory"           -> isMandatory
      )
    repo
      .getPOs(id)
      .map((m, o) => JsArray(m.map(toJson(_, isMandatory = true)) ++ o.map(toJson(_, isMandatory = false))))
  }

  def allModuleCore(): Future[Seq[ModuleCore]] =
    repo.allModuleCore()

  def allModuleCoreWithRelations(): Future[(Seq[ModuleCore], Map[UUID, Set[UUID]])] =
    repo.allModuleCoreWithRelations()

  def allNewlyCreated(): Future[Seq[ModuleCore]] =
    moduleCreationService.allAsModuleCore()

  def allMetadata(): Future[Seq[(Option[UUID], MetadataProtocol)]] =
    repo.all(Map.empty).map(_.map { case (module, _) => (module.id, module.metadata) })

  def allGenericModulesWithPOs(): Future[Seq[(ModuleCore, Seq[String])]] =
    repo.allGenericModulesWithPOs()

  def allNewlyCreatedGenericModulesWithPOs(): Future[Seq[(ModuleCore, Seq[String])]] =
    moduleCreationService.allGenericWithPOsAsModuleCore()

  def allGeneric(): Future[Seq[ModuleCore]] =
    for
      a <- repo.allGeneric()
      b <- moduleCreationService.allGeneric()
    yield a.concat(b).distinctBy(_.id)

  def allFromPO(po: String | Specialization, activeOnly: Boolean): Future[Seq[(ModuleProtocol, LocalDateTime)]] =
    repo.allFromPO(po, activeOnly)

  def allFromPOWithCompanion(po: String, activeOnly: Boolean): Future[Seq[(ModuleProtocol, Seq[(String, JsValue)])]] =
    for
      modules          <- allFromPO(po, activeOnly)
      companionContent <- moduleCompanionService.allFromModules(modules.map(_._1.id.get))
    yield {
      val companionsByModule = companionContent
        .collect {
          case (companion, Some(content)) => companion.module -> (companion.po, content)
        }
        .groupMap(_._1)(_._2)

      modules.map {
        case (module, _) =>
          module -> companionsByModule.getOrElse(module.id.get, Seq.empty)
      }
    }
}
