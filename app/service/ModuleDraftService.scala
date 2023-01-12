package service

import database.repo.{ModuleDraftRepository, UserBranchRepository}
import models.ModuleDraft

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ModuleDraftService @Inject() (
    private val repo: ModuleDraftRepository,
    private val userBranchRepository: UserBranchRepository,
    private implicit val ctx: ExecutionContext
) {
  def allFromBranch(branch: String) =
    repo.allFromBranch(branch)

  def update(moduleDraft: ModuleDraft) =
    for {
      exists <- userBranchRepository.exists(moduleDraft.branch)
      (_, res) <-
        if (exists) repo.createOrUpdate(moduleDraft)
        else
          Future.failed(
            new Throwable(s"branch ${moduleDraft.branch} doesn't exist")
          )
    } yield res
}
