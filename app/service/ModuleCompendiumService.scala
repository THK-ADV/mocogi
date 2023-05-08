package service

import database.repo.ModuleCompendiumRepository
import database.{MetadataOutput, ModuleCompendiumOutput}
import git.GitFilePath
import parsing.types.ModuleCompendium

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class Module(
    id: UUID,
    title: String,
    abbrev: String
)

trait ModuleCompendiumService {
  def createOrUpdateMany(
      entries: Seq[(GitFilePath, ModuleCompendium, LocalDateTime)]
  ): Future[Seq[ModuleCompendium]]
  def all(filter: Map[String, Seq[String]]): Future[Seq[ModuleCompendiumOutput]]
  def allIdsAndAbbrevs(): Future[Seq[(UUID, String)]]
  def allModules(filter: Map[String, Seq[String]]): Future[Seq[Module]]
  def allMetadata(filter: Map[String, Seq[String]]): Future[Seq[MetadataOutput]]
  def get(id: UUID): Future[ModuleCompendiumOutput]
  def paths(ids: Seq[UUID]): Future[Seq[(UUID, GitFilePath)]]
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

  override def allIdsAndAbbrevs() =
    repo.allIds()

  override def get(id: UUID) =
    repo.get(id)

  override def allModules(filter: Map[String, Seq[String]]) =
    repo.allPreview(filter).map(_.map(Module.tupled))

  override def paths(ids: Seq[UUID]) =
    repo.paths(ids)

  override def allMetadata(filter: Map[String, Seq[String]]) =
    repo.all(filter).map(_.map(_.metadata))
}
