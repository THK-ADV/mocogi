package catalog

import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

import catalog.PreviewMergeActor.CreateMergeRequest
import database.repo.ModuleCatalogGenerationRequestRepository
import git.api.GitMergeRequestApiService
import models.ModuleCatalogGenerationRequest
import models.Semester
import ops.FutureOps.Ops
import ops.LoggerOps
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.Props
import play.api.Logging

@deprecated
@Singleton
final class PreviewMergeActor(actor: ActorRef) {
  def createMergeRequest(semester: Semester): Unit =
    actor ! CreateMergeRequest(semester)
}

object PreviewMergeActor {
  case class CreateMergeRequest(semester: Semester) extends AnyVal

  def props(
      mergeRequestApi: GitMergeRequestApiService,
      moduleCatalogGenerationRequestRepo: ModuleCatalogGenerationRequestRepository,
      gitLabel: String,
      ctx: ExecutionContext
  ) =
    Props(
      new Impl(
        mergeRequestApi,
        moduleCatalogGenerationRequestRepo,
        gitLabel,
        ctx
      )
    )

  private class Impl(
      private val mergeRequestApi: GitMergeRequestApiService,
      private val moduleCatalogGenerationRequestRepo: ModuleCatalogGenerationRequestRepository,
      private val gitLabel: String,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging
      with LoggerOps {

    private def config = mergeRequestApi.config

    private def createMergeRequest(semester: Semester) =
      for {
        _ <- moduleCatalogGenerationRequestRepo
          .exists(semester)
          .abortIf(
            identity,
            s"module catalog generation request already defined for semester ${semester.id}"
          )
        _ <- mergeRequestApi
          .hasOpenedMergeRequests(config.draftBranch)
          .abortIf(
            identity,
            s"expected no opened merge request for ${config.draftBranch}"
          )
        (mrId, mrStatus) <- mergeRequestApi.create(
          config.draftBranch,
          config.mainBranch,
          s"Big Bang for ${semester.deLabel} ${semester.year}",
          "",
          needsApproval = false,
          List(gitLabel)
        )
        _ = logger.info(
          s"successfully created merge request with id ${mrId.value} and status ${mrStatus.id}"
        )
        _ <- moduleCatalogGenerationRequestRepo.create(
          ModuleCatalogGenerationRequest(mrId, semester.id, mrStatus)
        )
      } yield logger.info(
        s"successfully created generation request with id ${mrId.value} and semester ${semester.id}"
      )

    override def receive: Receive = {
      case CreateMergeRequest(semester) =>
        logger.info(
          s"start merging ${config.draftBranch} into ${config.mainBranch} for semester ${semester.id}"
        )
        createMergeRequest(semester).onComplete {
          case Success(_) => logger.info("finished!")
          case Failure(e) => logFailure(e)
        }
    }
  }
}
