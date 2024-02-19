package webhook

import akka.actor.{Actor, ActorRef, Props}
import git.GitChanges.CategorizedGitFilePaths
import git.api.GitFileDownloadService
import git.publisher.{CoreDataPublisher, ModulePublisher}
import git.{Branch, CommitId, GitChanges, GitConfig, GitFilePath}
import ops.LoggerOps
import play.api.Logging
import play.api.libs.json._

import java.time.LocalDateTime
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
case class GitPushEventHandler(private val value: ActorRef) {
  def handle(json: JsValue): Unit =
    value ! HandleEvent(json)
}

object GitPushEventHandler {
  private def invalidCommitId = "0000000000000000000000000000000000000000"

  def parseCommit(json: JsValue, key: String): JsResult[CommitId] =
    json
      .\(key)
      .validate[String]
      .filter(
        JsError(
          s"expected a real commit id for key '$key', but was: '$invalidCommitId'"
        )
      )(
        _ != invalidCommitId
      )
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
    for {
      branch <- parseBranch(json)
      afterCommit <- parseCommit(json, "after")
      _ <- parseCommit(json, "before")
      (added, modified, deleted, timestamp) <- parseFilesOfLastCommit(
        json,
        afterCommit
      )
    } yield (
      branch,
      GitChanges(added, modified, deleted, afterCommit, timestamp)
    )

  def props(
      downloadService: GitFileDownloadService,
      modulePublisher: ModulePublisher,
      coreDataPublisher: CoreDataPublisher,
      gitConfig: GitConfig,
      ctx: ExecutionContext
  ) = Props(
    new Impl(
      downloadService,
      modulePublisher,
      coreDataPublisher,
      gitConfig,
      ctx
    )
  )

  private final class Impl(
      downloadService: GitFileDownloadService,
      modulePublisher: ModulePublisher,
      coreDataPublisher: CoreDataPublisher,
      implicit val gitConfig: GitConfig,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging
      with LoggerOps {

    override def receive: Receive = { case HandleEvent(json) =>
      logger.info("start handling git push event")
      parse(json) match {
        case JsSuccess((branch, changes), _) =>
          branch.value match {
            case gitConfig.mainBranch.value =>
              downloadAndUpdateDatabase(branch, changes).onComplete {
                case Success(_) => logger.info("finished!")
                case Failure(e) => logFailure(e)
              }
            case _ =>
              logger.info(s"no action for branch $branch")
          }
        case JsError(errors) =>
          logUnhandedEvent(logger, errors)
      }
    }

    private def downloadAndUpdateDatabase(
        branch: Branch,
        changes: GitChanges[List[GitFilePath]]
    ) = {
      val CategorizedGitFilePaths(modules, core, _) = changes.categorized

      if (modules.nonEmpty) {
        logger.info(s"downloading modules files ${mkString(modules)}")
      }
      if (core.nonEmpty) {
        logger.info(s"downloading core files ${mkString(core)}")
      }

      for {
        modules <- Future.sequence(
          modules.map(path =>
            downloadService
              .downloadFileContent(path, branch)
              .collect { case Some(content) => path -> content }
          )
        )
        cores <- Future.sequence(
          core.map(path =>
            downloadService
              .downloadFileContent(path, branch)
              .collect { case Some(content) => path -> content }
          )
        )
      } yield {
        if (modules.nonEmpty) {
          logger.info(
            "publishing modules to subscribers ..."
          )
          modulePublisher.notifySubscribers(
            changes.copy(added = Nil, modified = modules)
          )
        }
        if (cores.nonEmpty) {
          logger.info(
            "publishing core files to subscribers ..."
          )
          coreDataPublisher.notifySubscribers(
            changes.copy(added = Nil, modified = cores)
          )
        }
      }
    }
  }
}
