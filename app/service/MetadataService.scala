package service

import database.repo.{MetadataOutput, MetadataRepository}
import git.GitFilePath
import validator.Metadata

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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
    repo.allIdsAndAbbrevs()
}
