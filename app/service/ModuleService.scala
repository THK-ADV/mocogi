package service

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.ModuleRepository
import models.core.Specialization
import models.ModuleCore
import models.ModuleProtocol
import ops.FutureOps.SeqOps
import parsing.types.Module
import play.api.libs.json.JsValue

@Singleton
final class ModuleService @Inject() (
    private val repo: ModuleRepository,
    private val moduleCreationService: ModuleCreationService,
    private val moduleCompanionService: ModuleCompanionService,
    private implicit val ctx: ExecutionContext
) {

  def createOrUpdateMany(
      modules: Seq[Module],
      timestamp: LocalDateTime
  ) =
    repo.createOrUpdateMany(modules, timestamp)

  def get(id: UUID) =
    repo.all(Map("id" -> Seq(id.toString))).single

  def allModuleCore(): Future[Seq[ModuleCore]] =
    repo.allModuleCore()

  def allNewlyCreated(): Future[Seq[ModuleCore]] =
    moduleCreationService.allAsModuleCore()

  def allMetadata() =
    repo.all(Map.empty).map(_.map(a => (a.id, a.metadata)))

  def allGenericModulesWithPOs(): Future[Seq[(ModuleCore, Seq[String])]] =
    repo.allGenericModulesWithPOs()

  def allNewlyCreatedGenericModulesWithPOs(): Future[Seq[(ModuleCore, Seq[String])]] =
    moduleCreationService.allGenericWithPOsAsModuleCore()

  def allFromMandatoryPO(po: String | Specialization): Future[Seq[ModuleProtocol]] =
    repo.allFromMandatoryPO(po)

  def allFromPO(po: String | Specialization, activeOnly: Boolean): Future[Seq[ModuleProtocol]] =
    repo.allFromPO(po, activeOnly)

  def allFromPOWithCompanion(po: String, activeOnly: Boolean): Future[Seq[(ModuleProtocol, Seq[(String, JsValue)])]] =
    for
      modules          <- allFromPO(po, activeOnly)
      companionContent <- moduleCompanionService.allFromModules(modules.map(_.id.get))
    yield modules.map { module =>
      val companion = companionContent.collect {
        case (companion, Some(c)) if companion.module == module.id.get => (companion.po, c)
      }
      (module, companion)
    }
}
