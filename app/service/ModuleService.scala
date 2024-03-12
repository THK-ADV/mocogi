package service

import database.repo.ModuleRepository
import ops.FutureOps.SeqOps
import parsing.types.Module

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

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

  def allFromPoMandatory(poId: String) =
    repo.all(Map("po_mandatory" -> Seq(poId)))

  def get(id: UUID) =
    repo.all(Map("id" -> Seq(id.toString))).single

  def allModuleCore(filter: Map[String, Seq[String]]) =
    repo.allModuleCore(filter)

  def allMetadata() =
    repo.all(Map.empty).map(_.map(_.metadata))
}
