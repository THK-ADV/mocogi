package service

import database.repo.{MetadataOutput, MetadataRepository}
import git.GitFilePath
import validator.Metadata

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait MetadataService {
  def create(metadata: Metadata, path: GitFilePath): Future[Metadata]
  def all(): Future[Seq[MetadataOutput]]
  def allIdsAndAbbrevs(): Future[Seq[(UUID, String)]]
}

@Singleton
final class MetadataServiceImpl @Inject() (
    private val repo: MetadataRepository,
    private implicit val ctx: ExecutionContext
) extends MetadataService {

  /*  def createOrUpdate(
      m: ParsedMetadata,
      path: GitFilePath
  ): Future[MetadataResult] =
    for {
      exists <- repo.exists(m)
      res <- if (exists) update(m, path) else create(m, path)
    } yield res

  def create(m: ParsedMetadata, path: GitFilePath): Future[MetadataResult] =
    repo.create(m, path)

  def update(m: ParsedMetadata, path: GitFilePath): Future[MetadataResult] =
    repo.update(m, path)

  def delete(path: GitFilePath): Future[Unit] =
    repo.delete(path)

  def all(): Future[Seq[(ParsedMetadata, GitFilePath)]] = repo.all()*/

  override def create(metadata: Metadata, path: GitFilePath) =
    repo.create(metadata, path)

  override def all() =
    repo.all()

  override def allIdsAndAbbrevs() =
    repo.allIdsAndAbbrevs()
}
