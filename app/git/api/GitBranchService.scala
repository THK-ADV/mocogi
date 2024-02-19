package git.api

import git.Branch
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitBranchService @Inject() (
    private val apiService: GitBranchApiService,
    implicit val ctx: ExecutionContext
) {

  def createPreviewBranch(): Future[Unit] =
    apiService.createBranch(
      apiService.config.draftBranch,
      apiService.config.mainBranch
    )

  def createModuleBranch(moduleId: UUID): Future[Branch] = {
    val branch = this.branch(moduleId)
    apiService
      .createBranch(branch, apiService.config.draftBranch)
      .map(_ => branch)
  }

  def deleteModuleBranch(moduleId: UUID): Future[Unit] = {
    val branch = this.branch(moduleId)
    apiService.deleteBranch(branch)
  }

  private def branch(module: UUID): Branch =
    Branch(module.toString)
}
