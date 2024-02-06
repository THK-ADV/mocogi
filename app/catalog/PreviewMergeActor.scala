package catalog

import akka.actor.{Actor, ActorRef}
import catalog.PreviewMergeActor.Merge
import git.api.GitMergeRequestApiService
import ops.LoggerOps
import play.api.Logging

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
final class PreviewMergeActor(actor: ActorRef) {
  def merge(): Unit =
    actor ! Merge
}

object PreviewMergeActor {
  case object Merge

  private class Impl(
      private val mergeRequestApi: GitMergeRequestApiService,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging
      with LoggerOps {

    private def config = mergeRequestApi.config

    private def ensureNonPendingMergeRequests(): Future[Unit] =
      mergeRequestApi
        .hasOpenedMergeRequests(config.draftBranch)
        .flatMap(b =>
          if (b)
            Future.failed(
              new Throwable(
                s"expected no opened merge request for ${config.draftBranch}"
              )
            )
          else Future.unit
        )

    // TODO
//    private def mergePreviewBranch(): Future[Unit] =
//      for {
//        (mergeRequestId, _) <- mergeRequestApi.create(
//          config.draftBranch,
//          config.mainBranch,
//          "Big Bang",
//          "",
//          needsApproval = false,
//          Nil
//        )
//        _ <- mergeRequestApi.canBeMerged(mergeRequestId)
//        _ <- mergeRequestApi.merge(mergeRequestId)
//      } yield ()

    private def merge(): Future[Unit] =
      for {
        _ <- ensureNonPendingMergeRequests()
//        _ <- mergePreviewBranch()
      } yield ()

    override def receive: Receive = { case Merge =>
      logger.info(
        s"start merging ${config.draftBranch} branch into ${config.mainBranch} branch"
      )
      merge() onComplete {
        case Success(_) =>
          logger.info("successfully merged!")
        case Failure(e) =>
          logFailure(e)
      }
    }
  }
}
