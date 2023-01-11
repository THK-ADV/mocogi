package service

import database.MetadataOutput
import database.repo.MetadataRepository
import git.GitFilePath
import validator.Metadata

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class Module(
    id: UUID,
    title: String,
    abbrev: String
)

trait MetadataService {
  def create(
      metadata: Metadata,
      path: GitFilePath,
      timestamp: LocalDateTime
  ): Future[Metadata]
  def createOrUpdate(
      metadata: Metadata,
      path: GitFilePath,
      timestamp: LocalDateTime
  ): Future[Metadata]
  def all(filter: Map[String, Seq[String]]): Future[Seq[MetadataOutput]]
  def allIdsAndAbbrevs(): Future[Seq[(UUID, String)]]
  def allModules(filter: Map[String, Seq[String]]): Future[Seq[Module]]
  def get(id: UUID): Future[MetadataOutput]
}

@Singleton
final class MetadataServiceImpl @Inject() (
    private val repo: MetadataRepository,
    private implicit val ctx: ExecutionContext
) extends MetadataService {

  override def createOrUpdate(
      metadata: Metadata,
      path: GitFilePath,
      timestamp: LocalDateTime
  ) =
    for {
      exists <- repo.exists(metadata)
      res <-
        if (exists) repo.update(metadata, path, timestamp)
        else create(metadata, path, timestamp)
    } yield res

  override def create(
      metadata: Metadata,
      path: GitFilePath,
      timestamp: LocalDateTime
  ) =
    repo.create(metadata, path, timestamp)

  override def all(filter: Map[String, Seq[String]]) =
    repo.all(filter)

  override def allIdsAndAbbrevs() =
    repo.allIds()

  override def get(id: UUID) =
    repo.get(id)

  override def allModules(filter: Map[String, Seq[String]]) =
    repo.allPreview(filter).map(_.map(Module.tupled))
}
