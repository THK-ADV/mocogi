package git.api

import models.Branch

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitBranchService @Inject() (
    private val apiService: GitBranchApiService,
    implicit val ctx: ExecutionContext
) {

  def createBranch(moduleId: UUID): Future[Branch] = {
    val branch = this.branch(moduleId)
    apiService.createBranch(branch).map(_ => branch)
  }

  def deleteBranch(moduleId: UUID): Future[Unit] = {
    val branch = this.branch(moduleId)
    apiService.deleteBranch(branch)
  }

  private def branch(module: UUID): Branch =
    Branch(module.toString)
}
