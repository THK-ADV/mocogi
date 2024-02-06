package service

import database.repo.ModuleRepository
import models.{MetadataProtocol, ModuleCore, ModuleProtocol}
import ops.FutureOps.SeqOps
import parsing.types.Module

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait ModuleService {
  def createOrUpdateMany(
      modules: Seq[Module],
      timestamp: LocalDateTime
  ): Future[Seq[Unit]]
  def all(filter: Map[String, Seq[String]]): Future[Seq[ModuleProtocol]]
  def allModuleCore(filter: Map[String, Seq[String]]): Future[Seq[ModuleCore]]
  def allMetadata(
      filter: Map[String, Seq[String]]
  ): Future[Seq[MetadataProtocol]]
  def get(id: UUID): Future[ModuleProtocol]
  def getOrNull(id: UUID): Future[Option[ModuleProtocol]]
}

@Singleton
final class ModuleServiceImpl @Inject() (
    private val repo: ModuleRepository,
    private implicit val ctx: ExecutionContext
) extends ModuleService {

  override def createOrUpdateMany(
      modules: Seq[Module],
      timestamp: LocalDateTime
  ) =
    repo.createOrUpdateMany(modules, timestamp)

  override def all(filter: Map[String, Seq[String]]) =
    repo.all(filter)

  override def get(id: UUID) =
    repo.all(Map("id" -> Seq(id.toString))).single

  override def getOrNull(id: UUID) =
    repo.all(Map("id" -> Seq(id.toString))).map(_.headOption)

  override def allModuleCore(filter: Map[String, Seq[String]]) =
    repo.allModuleCore(filter)

  // TODO inefficient implementation
  override def allMetadata(filter: Map[String, Seq[String]]) =
    repo.all(filter).map(_.map(_.metadata))
}
