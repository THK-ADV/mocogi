package webhook

import akka.actor.{Actor, ActorRef}
import database.repo.{ModuleDraftRepository, ModuleReviewRepository}
import git.GitConfig
import git.api.GitMergeRequestApiService
import models.{Branch, MergeRequestId}
import ops.LoggerOps
import play.api.Logging
import play.api.libs.json._

import java.util.UUID
import javax.inject.Singleton
import scala.collection.IndexedSeq
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
case class GitMergeEventHandler(private val value: ActorRef) {
  def handle(json: JsValue): Unit =
    value ! HandleEvent(json)
}

object GitMergeEventHandler {
  private type MergeStatus = String
  private type State = String
  private type Action = String
  private type Labels = IndexedSeq[String]
  private type ParseResult =
    (MergeRequestId, MergeStatus, State, Action, Branch, Branch, Labels)

  private class Impl(
      gitConfig: GitConfig,
      moduleReviewRepository: ModuleReviewRepository,
      moduleDraftRepository: ModuleDraftRepository,
      mergeRequestApiService: GitMergeRequestApiService,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging
      with LoggerOps {

    private def parse(json: JsValue): JsResult[ParseResult] = {
      val attrs = json.\("object_attributes")
      for {
        mrId <- attrs.\("iid").validate[Int].map(MergeRequestId.apply)
        mergeStatus <- attrs.\("detailed_merge_status").validate[String]
        state <- attrs.\("state").validate[String]
        action <- attrs.\("action").validate[String]
        sourceBranch <- attrs
          .\("source_branch")
          .validate[String]
          .map(Branch.apply)
        targetBranch <- attrs
          .\("target_branch")
          .validate[String]
          .map(Branch.apply)
        labels <- attrs
          .\("labels")
          .validate[JsArray]
          .map(_.value.collect {
            case title if title.\("title").isDefined =>
              title.\("title").validate[String].get
          })
      } yield (
        mrId,
        mergeStatus,
        state,
        action,
        sourceBranch,
        targetBranch,
        labels
      )
    }

    private def removeDraft(id: UUID): Future[Unit] = {
      logger.info(s"deleting module draft with id $id")
      for {
        res1 <- moduleReviewRepository.delete(id)
        res2 <- moduleDraftRepository.delete(id)
      } yield logger.info(
        s"successfully deleted $res1 module reviews and $res2 module drafts"
      )
    }

    private def logAbortion(result: ParseResult): Unit = {
      logger.info(s"""unable to handle event
                     |- merge request id: ${result._1.value}
                     |- merge status: ${result._2}
                     |- state: ${result._3}
                     |- action: ${result._4}
                     |- source: ${result._5.value}
                     |- target ${result._6.value}
                     |- labels: ${result._7}""".stripMargin)
    }

    override def receive: Receive = { case HandleEvent(json) =>
      logger.info("start handling git merge event")
      parse(json) match {
        case JsSuccess(result, _) =>
          val targetBranch = result._6
          val sourceBranch = result._5
          val mrId = result._1
          val mergeStatus = result._2
          val state = result._3
          val action = result._4
          if (targetBranch == gitConfig.draftBranch) {
            val res = for {
              moduleId <- Future.fromTry(
                Try(UUID.fromString(sourceBranch.value))
              )
              res <- (mergeStatus, state, action) match {
                case ("mergeable", "opened", "open") => merge(mrId, moduleId)
                case ("not_open", "merged", "merge") => removeDraft(moduleId)
                case _                               => Future.unit
              }
            } yield res
            res.onComplete {
              case Success(_) => logger.info("finished!")
              case Failure(e) => logFailure(e)
            }
          } else {
            logAbortion(result)
          }
        case JsError(errors) =>
          logUnhandedEvent(logger, errors)
      }
    }

    private def merge(mrId: MergeRequestId, moduleId: UUID): Future[Unit] =
      for {
        mrStatus <- mergeRequestApiService.merge(mrId)
        _ <- moduleDraftRepository.updateMergeRequestStatus(
          moduleId,
          mrStatus
        )
      } yield ()
  }
}
