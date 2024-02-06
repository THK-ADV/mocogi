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

  def all(filter: Map[String, Seq[String]]) =
    repo.all(filter)

  def get(id: UUID) =
    repo.all(Map("id" -> Seq(id.toString))).single

  def getOrNull(id: UUID) =
    repo.all(Map("id" -> Seq(id.toString))).map(_.headOption)

  def allModuleCore(filter: Map[String, Seq[String]]) =
    repo.allModuleCore(filter)

  // TODO inefficient implementation
  def allMetadata(filter: Map[String, Seq[String]]) =
    repo.all(filter).map(_.map(_.metadata))
}
