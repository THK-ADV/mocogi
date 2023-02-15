package git.webhook

import akka.actor.{Actor, ActorRef, Props}
import database.repo.UserBranchRepository
import git.subscriber.ModuleCompendiumSubscribers
import git.webhook.GitMergeEventHandlingActor.HandleMergeEvent
import git.{GitConfig, GitFilePath}
import models.{ModuleDraftStatus, UserBranch, ValidModuleDraft}
import play.api.Logging
import play.api.libs.json.{JsResult, JsValue}
import service.ModuleDraftService

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object GitMergeEventHandlingActor {
  def parseIsMerge(json: JsValue): JsResult[Boolean] =
    json.\("object_attributes").\("action").validate[String].map(_ == "merge")

  def parseMergeRequestId(json: JsValue): JsResult[Int] =
    json.\("object_attributes").\("iid").validate[Int]

  def parseSourceBranch(json: JsValue): JsResult[String] =
    json.\("object_attributes").\("source_branch").validate[String]

  def parseLastCommitId(json: JsValue): JsResult[String] =
    json.\("object_attributes").\("last_commit").\("id").validate[String]

  private case class HandleMergeEvent(json: JsValue)

  def props(
      userBranchRepository: UserBranchRepository,
      moduleDraftService: ModuleDraftService,
      subscribers: ModuleCompendiumSubscribers,
      gitConfig: GitConfig,
      ctx: ExecutionContext
  ) =
    Props(
      new GitMergeEventHandlingActorImpl(
        userBranchRepository,
        moduleDraftService,
        subscribers,
        gitConfig,
        ctx
      )
    )

  private final class GitMergeEventHandlingActorImpl(
      userBranchRepository: UserBranchRepository,
      moduleDraftService: ModuleDraftService,
      subscribers: ModuleCompendiumSubscribers,
      implicit val gitConfig: GitConfig,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {
    import ops.JsResultOps._

    private type MergeRequestID = Int
    private type SourceBranch = String
    private type LastCommitId = String

    override def receive: Receive = { case HandleMergeEvent(json) =>
      go(json) onComplete {
        case Success(_) =>
          logger.info("successfully handled git merge event")
        case Failure(t) =>
          logger.error(
            s"""failed to handle git merge event
                 |  - message: ${t.getMessage}
                 |  - trace: ${t.getStackTrace.mkString(
                "\n           "
              )}""".stripMargin
          )
      }
    }

    private def parse(
        json: JsValue
    ): Try[Option[(MergeRequestID, SourceBranch, LastCommitId)]] = {
      val parseRes = for {
        isMerge <- parseIsMerge(json)
        mergeRequestId <- parseMergeRequestId(json)
        sourceBranch <- parseSourceBranch(json)
        lastCommitId <- parseLastCommitId(json)
      } yield (isMerge, mergeRequestId, sourceBranch, lastCommitId)

      parseRes.toTry.map { case (isMerge, mrid, sb, lcid) =>
        Option.when(isMerge)((mrid, sb, lcid))
      }
    }

    private def findMatchingBranch(
        xs: Seq[UserBranch],
        mergeRequestId: MergeRequestID,
        sourceBranch: SourceBranch,
        lastCommitId: LastCommitId
    ): Option[UserBranch] =
      xs.find(b =>
        b.branch == sourceBranch &&
          b.mergeRequestId.fold(false)(_ == mergeRequestId) &&
          b.commitId.fold(false)(_ == lastCommitId)
      )

    private def notifySubscribers(
        drafts: Seq[ValidModuleDraft],
        commitId: LastCommitId
    ): Unit = // TODO maybe this should be done synchronously in one transaction
      drafts.foreach { d =>
        val mc = moduleDraftService.parseModuleCompendium(d.json)
        d.status match {
          case ModuleDraftStatus.Added =>
            subscribers.added(
              commitId,
              d.lastModified,
              GitFilePath(d),
              mc
            )
          case ModuleDraftStatus.Modified =>
            subscribers.modified(
              commitId,
              d.lastModified,
              GitFilePath(d),
              mc
            )
        }
      }

    private def go(json: JsValue): Future[Unit] =
      Future.fromTry(parse(json)).flatMap {
        case Some((mergeRequestId, sourceBranch, lastCommitId)) =>
          for {
            userBranches <- userBranchRepository.allWithOpenedMergeRequests()
            res <- findMatchingBranch(
              userBranches,
              mergeRequestId,
              sourceBranch,
              lastCommitId
            ) match {
              case Some(branch) =>
                for {
                  drafts <- moduleDraftService.validDrafts(branch.branch)
                  _ = notifySubscribers(drafts, lastCommitId)
                  _ <- moduleDraftService.delete(branch)
                  _ <- userBranchRepository.delete(branch.user)
                } yield ()
              case None =>
                Future.unit
            }
          } yield res
        case None =>
          Future.unit
      }
  }
}

@Singleton
case class GitMergeEventHandlingActor(private val value: ActorRef) {
  def handle(json: JsValue): Unit =
    value ! HandleMergeEvent(json)
}
