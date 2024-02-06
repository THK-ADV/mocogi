package service

import database.repo.ModuleRepository
import database.{MetadataOutput, ModuleOutput}
import git.GitFilePath
import models.ModuleCore
import ops.FutureOps.SeqOps
import parsing.types.Module

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait ModuleService {
  def createOrUpdateMany(
      entries: Seq[(GitFilePath, Module, LocalDateTime)]
  ): Future[Seq[Unit]]
  def all(filter: Map[String, Seq[String]]): Future[Seq[ModuleOutput]]
  def allModuleCore(filter: Map[String, Seq[String]]): Future[Seq[ModuleCore]]
  def allMetadata(filter: Map[String, Seq[String]]): Future[Seq[MetadataOutput]]
  def get(id: UUID): Future[ModuleOutput]
  def getOrNull(id: UUID): Future[Option[ModuleOutput]]
}

@Singleton
final class ModuleServiceImpl @Inject() (
    private val repo: ModuleRepository,
    private implicit val ctx: ExecutionContext
) extends ModuleService {

  override def createOrUpdateMany(
      entries: Seq[(GitFilePath, Module, LocalDateTime)]
  ) =
    repo.createOrUpdateMany(entries)

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
