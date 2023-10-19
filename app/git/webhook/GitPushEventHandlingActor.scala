package git.webhook

import akka.actor.{Actor, ActorRef, Props}
import git.api.GitFileDownloadService
import git.publisher.{CoreDataPublisher, ModuleCompendiumPublisher}
import git.webhook.GitPushEventHandlingActor.HandleMergeEvent
import git.{GitChanges, GitConfig, GitFilePath}
import models.{Branch, CommitId}
import play.api.Logging
import play.api.libs.json.{JsArray, JsError, JsResult, JsValue}
import service.ModuleDraftService

import java.time.LocalDateTime
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object GitPushEventHandlingActor {
  private case class HandleMergeEvent(json: JsValue) extends AnyVal

  def parseAfterCommit(json: JsValue): JsResult[CommitId] =
    json
      .\("after")
      .validate[String]
      .map(CommitId.apply)

  def parseBranch(json: JsValue): JsResult[Branch] =
    json
      .\("ref")
      .validate[String]
      .map(_.split("/").last)
      .map(Branch.apply)

  def parseFilesOfLastCommit(
      json: JsValue,
      lastCommit: CommitId
  ) =
    for {
      commits <- json.\("commits").validate[JsArray]
      mergeCommit <- commits.value.find(
        _.\("id").validate[String].map(_ == lastCommit.value).getOrElse(false)
      ) match {
        case Some(commit) =>
          for {
            added <- commit.\("added").validate[List[String]]
            modified <- commit.\("modified").validate[List[String]]
            removed <- commit.\("removed").validate[List[String]]
            timestamp <- commit.\("timestamp").validate[LocalDateTime]
          } yield (
            added.map(GitFilePath.apply),
            modified.map(GitFilePath.apply),
            removed.map(GitFilePath.apply),
            timestamp
          )
        case None => JsError(s"expected commit with id ${lastCommit.value}")
      }
    } yield mergeCommit

  def parse(json: JsValue) =
    JsResult.toTry(
      for {
        branch <- parseBranch(json)
        afterCommit <- parseAfterCommit(json)
        (added, modified, deleted, timestamp) <- parseFilesOfLastCommit(
          json,
          afterCommit
        )
      } yield (
        branch,
        GitChanges(added, modified, deleted, afterCommit, timestamp)
      )
    )

  def props(
      moduleDraftService: ModuleDraftService,
      downloadService: GitFileDownloadService,
      moduleCompendiumPublisher: ModuleCompendiumPublisher,
      coreDataPublisher: CoreDataPublisher,
      gitConfig: GitConfig,
      ctx: ExecutionContext
  ) = Props(
    new Impl(
      moduleDraftService,
      downloadService,
      moduleCompendiumPublisher,
      coreDataPublisher,
      gitConfig,
      ctx
    )
  )

  private final class Impl(
      moduleDraftService: ModuleDraftService,
      downloadService: GitFileDownloadService,
      moduleCompendiumPublisher: ModuleCompendiumPublisher,
      coreDataPublisher: CoreDataPublisher,
      implicit val gitConfig: GitConfig,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {

    override def receive: Receive = { case HandleMergeEvent(json) =>
      go(json) onComplete {
        case Success(_) =>
          logger.info("successfully handled git push event")
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

    private def mkString[A](xs: Seq[A]): String =
      xs.mkString("\n\t- ", "\n\t- ", "")

    private def go(json: JsValue): Future[Unit] =
      Future.fromTry(parse(json)).flatMap { case (branch, changes) =>
        branch.value match {
          case gitConfig.draftBranch => removeDrafts(changes)
          case gitConfig.mainBranch =>
            downloadAndUpdateDatabase(branch, changes)
          case _ =>
            logger.info(s"no action for branch $branch")
            Future.unit
        }
      }

    private def downloadAndUpdateDatabase(
        branch: Branch,
        changes: GitChanges[List[GitFilePath]]
    ) = {
      val (modules, cores) =
        (changes.added ::: changes.modified).partition(_.isModule)

      if (modules.nonEmpty) {
        logger.info(s"downloading modules files ${mkString(modules)}")
      }

      if (cores.nonEmpty) {
        logger.info(s"downloading core files ${mkString(cores)}")
      }

      for {
        modules <- Future.sequence(
          modules.map(path =>
            downloadService
              .downloadFileContent(path, branch)
              .map(path -> _)
          )
        )
        cores <- Future.sequence(
          cores.map(path =>
            downloadService
              .downloadFileContent(path, branch)
              .map(path -> _)
          )
        )
      } yield {
        if (modules.nonEmpty) {
          logger.info(
            s"publishing modules to subscribers ..."
          )
          moduleCompendiumPublisher.notifySubscribers(
            changes.copy(added = Nil, modified = modules)
          )
        }
        if (cores.nonEmpty) {
          logger.info(
            s"publishing core files to subscribers ..."
          )
          coreDataPublisher.notifySubscribers(
            changes.copy(added = Nil, modified = cores)
          )
        }
      }
    }

    private def removeDrafts(
        changes: GitChanges[List[GitFilePath]]
    ): Future[Unit] = {
      val modules = (changes.added ::: changes.modified)
        .map(_.moduleId)
        .collect { case Some(module) => module }
      logger.info(s"deleting module drafts ${modules.mkString(",")} ...")
      moduleDraftService
        .deleteDrafts(modules)
        .map(_ =>
          logger.info(
            s"successfully deleted module drafts: ${modules.mkString(",")}"
          )
        )
    }
  }
}

@Singleton
case class GitPushEventHandlingActor(private val value: ActorRef) {
  def handle(json: JsValue): Unit =
    value ! HandleMergeEvent(json)
}
