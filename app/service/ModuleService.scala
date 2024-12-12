package service

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.ModuleRepository
import ops.FutureOps.SeqOps
import parsing.types.Module

@Singleton
final class ModuleService @Inject() (
    private val repo: ModuleRepository,
    private implicit val ctx: ExecutionContext
) {

  def createOrUpdateMany(
      modules: Seq[Module],
      timestamp: LocalDateTime
  ) =
    repo.createOrUpdateMany(modules, timestamp)

  def get(id: UUID) =
    repo.all(Map("id" -> Seq(id.toString))).single

  def allModuleCore(filter: Map[String, Seq[String]]) =
    repo.allModuleCore(filter)

  def allMetadata() =
    repo.all(Map.empty).map(_.map(a => (a.id, a.metadata)))

  def allGenericModules() =
    repo.allGenericModules()
}
