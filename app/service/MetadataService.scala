package service

import database.repo.{MetadataOutput, MetadataRepository}
import git.GitFilePath
import validator.Metadata

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class MetadataPreview(
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
  def all(): Future[Seq[MetadataOutput]]
  def allIdsAndAbbrevs(): Future[Seq[(UUID, String)]]
  def allOfUser(user: String): Future[Seq[MetadataOutput]]
  def allPreviewOfUser(user: String): Future[Seq[MetadataPreview]]
  def allPreview(): Future[Seq[MetadataPreview]]
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

  override def all() =
    repo.all()

  override def allIdsAndAbbrevs() =
    repo.allIds()

  override def allOfUser(user: String) =
    repo.allOfUser(user)

  override def allPreviewOfUser(user: String) =
    repo.allPreviewOfUser(user).map(_.map(MetadataPreview.tupled))

  override def get(id: UUID) =
    repo.get(id)

  override def allPreview() =
    repo.allPreview().map(_.map(MetadataPreview.tupled))
}
