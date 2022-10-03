package service

import database.repo.MetadataRepository
import database.table.{
  AssessmentMethodMetadataDbEntry,
  MetadataDbEntry,
  ResponsibilityDbEntry
}
import git.GitFilePath
import parsing.types.ParsedMetadata

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class MetadataService @Inject() (
    private val repo: MetadataRepository,
    private implicit val ctx: ExecutionContext
) {

  type MetadataResult = (
      MetadataDbEntry,
      List[ResponsibilityDbEntry],
      List[AssessmentMethodMetadataDbEntry]
  )

  def createOrUpdate(m: ParsedMetadata, path: GitFilePath): Future[MetadataResult] =
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

  def all(): Future[Seq[(ParsedMetadata, GitFilePath)]] = repo.all()
}
