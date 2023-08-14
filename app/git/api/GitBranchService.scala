package git.api

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class GitBranchService @Inject() (
    private val apiService: GitBranchApiService,
    implicit val ctx: ExecutionContext
) {
  def branchForUser(username: String): Future[Option[String]] =
    ???
//    userBranchRepository.branchForUser(username)

  def createBranch(moduleId: UUID): Future[String] =
    Future.successful(moduleId.toString)
//    for {
//      exists <- userBranchRepository.existsByUser(user)
//      res <-
//        if (exists)
//          Future.failed(
//            new Throwable(s"branch for user $user already exists")
//          )
//        else
//          for {
//            branch <- apiService.createBranch(user)
//            res <- userBranchRepository.create(branch)
//          } yield res
//    } yield res

  def deleteBranch(moduleId: UUID): Future[Unit] =
    ???
//    for {
//      exists <- userBranchRepository.existsByUser(user)
//      res <-
//        if (!exists)
//          Future.failed(
//            new Throwable(s"branch for user $user doesn't exists")
//          )
//        else
//          for {
//            _ <- apiService.deleteBranch(user)
//            d <- userBranchRepository.delete(user) if d > 0
//          } yield ()
//    } yield res
}
