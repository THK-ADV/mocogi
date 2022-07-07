package service

import database.repo.MetadataRepository
import database.table.{
  AssessmentMethodMetadataDbEntry,
  MetadataDbEntry,
  ResponsibilityDbEntry
}
import git.GitFilePath
import parsing.types.Metadata

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
final class MetadataService @Inject() (private val repo: MetadataRepository) {

  type MetadataResult = (
      MetadataDbEntry,
      List[ResponsibilityDbEntry],
      List[AssessmentMethodMetadataDbEntry]
  )

  def create(m: Metadata, path: GitFilePath): Future[MetadataResult] =
    repo.create(m, path)

  def update(m: Metadata, path: GitFilePath): Future[MetadataResult] =
    repo.update(m, path)

  def delete(path: GitFilePath): Future[Unit] =
    repo.delete(path)

  def all(): Future[Seq[(Metadata, GitFilePath)]] = repo.all()
}
