package service

import database.repo.ModuleCompendiumRepository
import database.{MetadataOutput, ModuleCompendiumOutput}
import git.GitFilePath
import models.Module
import ops.FutureOps.SeqOps
import parsing.types.ModuleCompendium

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait ModuleCompendiumService {
  def createOrUpdateMany(
      entries: Seq[(GitFilePath, ModuleCompendium, LocalDateTime)]
  ): Future[Seq[ModuleCompendium]]
  def all(filter: Map[String, Seq[String]]): Future[Seq[ModuleCompendiumOutput]]
  def allModules(filter: Map[String, Seq[String]]): Future[Seq[Module]]
  def allMetadata(filter: Map[String, Seq[String]]): Future[Seq[MetadataOutput]]
  def get(id: UUID): Future[ModuleCompendiumOutput]
  def getOrNull(id: UUID): Future[Option[ModuleCompendiumOutput]]
}

@Singleton
final class ModuleCompendiumServiceImpl @Inject() (
    private val repo: ModuleCompendiumRepository,
    private implicit val ctx: ExecutionContext
) extends ModuleCompendiumService {

  override def createOrUpdateMany(
      entries: Seq[(GitFilePath, ModuleCompendium, LocalDateTime)]
  ) =
    repo.createOrUpdateMany(entries)

  override def all(filter: Map[String, Seq[String]]) =
    repo.all(filter)

  override def get(id: UUID) =
    repo.all(Map("id" -> Seq(id.toString))).single

  override def getOrNull(id: UUID) =
    repo.all(Map("id" -> Seq(id.toString))).map(_.headOption)

  override def allModules(filter: Map[String, Seq[String]]) =
    repo.allPreview(filter)

  override def allMetadata(filter: Map[String, Seq[String]]) =
    repo.all(filter).map(_.map(_.metadata))
}
