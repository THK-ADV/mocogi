package git.webhook

import akka.actor.{Actor, ActorRef, Props}
import git.subscriber.ModuleCompendiumSubscribers
import git.webhook.GitMergeEventHandlingActor.HandleMergeEvent
import git.{GitConfig, GitFilePath}
import models.{MergeRequestId, ModuleDraft}
import play.api.Logging
import play.api.libs.json.{JsResult, JsValue}
import service.ModuleDraftService

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object GitMergeEventHandlingActor {
  def parseIsMerge(json: JsValue): JsResult[Boolean] =
    json
      .\("object_attributes")
      .\("action")
      .validate[String]
      .map(_ == "merge")

  def parseMergeRequestId(json: JsValue): JsResult[MergeRequestId] =
    json
      .\("object_attributes")
      .\("iid")
      .validate[Int]
      .map(MergeRequestId.apply)

  private case class HandleMergeEvent(json: JsValue)

  def props(
      moduleDraftService: ModuleDraftService,
      subscribers: ModuleCompendiumSubscribers,
      gitConfig: GitConfig,
      ctx: ExecutionContext
  ) =
    Props(
      new GitMergeEventHandlingActorImpl(
        moduleDraftService,
        subscribers,
        gitConfig,
        ctx
      )
    )

  private final class GitMergeEventHandlingActorImpl(
      moduleDraftService: ModuleDraftService,
      subscribers: ModuleCompendiumSubscribers,
      implicit val gitConfig: GitConfig,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {

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

    private def parse(json: JsValue): Try[Option[MergeRequestId]] = {
      val parseRes = for {
        isMerge <- parseIsMerge(json)
        mergeRequestId <- parseMergeRequestId(json)
      } yield (isMerge, mergeRequestId)

      JsResult.toTry(parseRes).map { case (isMerge, mrId) =>
        Option.when(isMerge)(mrId)
      }
    }

    private def notifySubscribers(drafts: Seq[ModuleDraft]): Unit =
      subscribers.createdOrUpdated(
        drafts.map(d =>
          (
            GitFilePath(d),
            moduleDraftService.parseModuleCompendium(d.moduleCompendium),
            d.lastModified
          )
        )
      )

    private def go(json: JsValue): Future[Unit] =
      Future.fromTry(parse(json)).flatMap {
        case Some(mergeRequestId) =>
          moduleDraftService.getByMergeRequest(mergeRequestId).map { drafts =>
            if (drafts.nonEmpty) notifySubscribers(drafts)
            else
              throw new Throwable(
                s"expected one draft for merge request ${mergeRequestId.value}"
              )
          }
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
