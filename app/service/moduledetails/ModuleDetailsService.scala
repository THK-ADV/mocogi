package service.moduledetails

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.ModuleDetailRepository
import git.api.GitCommitService
import git.api.GitFileService
import git.GitFilePath
import ops.or
import service.pipeline.MetadataPipeline
import service.pipeline.Print
import service.ModuleDraftService

@Singleton
final class ModuleDetailsService @Inject() (
    repository: ModuleDetailRepository,
    gitFileService: GitFileService,
    gitCommitService: GitCommitService,
    draftService: ModuleDraftService,
    pipeline: MetadataPipeline,
    implicit val ctx: ExecutionContext
) {

  private val gitConfig = gitFileService.config

  def published(id: UUID) = repository.getModuleDetails(id)

  def latest(id: UUID): Future[Option[ModuleDetails]] =
    latestPrint(id).flatMap {
      case Some((print, lastModified)) =>
        pipeline.parseValidate(print).flatMap(repository.assemble(_, lastModified)).map(Some(_))
      case None => Future.successful(None)
    }

  private def latestPrint(moduleId: UUID): Future[Option[(Print, LocalDateTime)]] =
    draftService
      .getByModuleOpt(moduleId)
      .map(_.map(draft => draft.print -> draft.lastModified))
      .or(getFromPreview(moduleId))

  private def getFromPreview(moduleId: UUID): Future[Option[(Print, LocalDateTime)]] = {
    val path = GitFilePath(moduleId)(gitConfig)
    gitFileService.download(path, gitConfig.draftBranch).flatMap {
      case Some((content, Some(commit))) =>
        gitCommitService
          .getCommitDate(commit.value)
          .map(lastModified => Some(Print(content.value) -> lastModified))
      case Some((content, None)) =>
        gitCommitService
          .getCommitDate(path, gitConfig.draftBranch)
          .map(_.map(lastModified => Print(content.value) -> lastModified))
      case None => Future.successful(None)
    }
  }
}
