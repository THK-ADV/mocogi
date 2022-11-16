package service

import database.repo.{MetadataOutput, MetadataRepository}
import git.GitFilePath
import validator.Metadata

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait MetadataService {
  def create(metadata: Metadata, path: GitFilePath): Future[Metadata]
  def createOrUpdate(metadata: Metadata, path: GitFilePath): Future[Metadata]
  def all(): Future[Seq[MetadataOutput]]
  def allIdsAndAbbrevs(): Future[Seq[(UUID, String)]]
}

@Singleton
final class MetadataServiceImpl @Inject() (
    private val repo: MetadataRepository,
    private implicit val ctx: ExecutionContext
) extends MetadataService {

  override def createOrUpdate(metadata: Metadata, path: GitFilePath) =
    for {
      exists <- repo.exists(metadata)
      res <- if (exists) repo.update(metadata, path) else create(metadata, path)
    } yield res

  override def create(metadata: Metadata, path: GitFilePath) =
    repo.create(metadata, path)

  override def all() =
    repo.all()

  override def allIdsAndAbbrevs() =
    repo.allIdsAndAbbrevs()
}
