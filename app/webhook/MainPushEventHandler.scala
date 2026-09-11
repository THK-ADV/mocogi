package webhook

import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Named

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import git.*
import git.api.GitCommitService
import git.api.GitFileService
import git.publisher.ModulePublisher
import logging.errorC
import logging.infoC
import logging.warnC
import logging.CorrelationId
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.ActorRef
import play.api.libs.json.*
import play.api.Logging

final class MainPushEventHandler @Inject() (
    downloadService: GitFileService,
    commitService: GitCommitService,
    @Named("ModulePublisher") modulePublisher: ActorRef,
    implicit val gitConfig: GitConfig,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  private def invalidCommitId = "0000000000000000000000000000000000000000"

  private def parseCommit(json: JsValue, key: String): JsResult[CommitId] =
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

  private def parseFilesOfLastCommit(json: JsValue, lastCommit: CommitId) =
    for {
      commits     <- json.\("commits").validate[JsArray]
      mergeCommit <- commits.value.find(_.\("id").validate[String].map(_ == lastCommit.value).getOrElse(false)) match {
        case Some(commit) =>
          for {
            added     <- commit.\("added").validate[List[String]]
            modified  <- commit.\("modified").validate[List[String]]
            removed   <- commit.\("removed").validate[List[String]]
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

  private def parse(json: JsValue)(implicit gitConfig: GitConfig) =
    for {
      branch                                <- parseBranch(json)
      afterCommit                           <- parseCommit(json, "after")
      _                                     <- parseCommit(json, "before")
      (added, modified, deleted, timestamp) <- parseFilesOfLastCommit(json, afterCommit)
    } yield (
      branch,
      GitChanges(toGitFiles(added, modified, deleted, timestamp), afterCommit)
    )

  private def toGitFiles(
      added: List[GitFilePath],
      modified: List[GitFilePath],
      deleted: List[GitFilePath],
      timestamp: LocalDateTime
  )(implicit gitConfig: GitConfig): List[GitFile] = {
    val builder = ListBuffer.empty[GitFile]
    added.foreach { path =>
      val status = GitFileStatus.Added
      builder += path.fold(
        GitFile.ModuleFile(path, _, status, timestamp),
        GitFile.ModuleCatalogFile(path, status),
        GitFile.Other(path, status)
      )
    }
    modified.foreach { path =>
      val status = GitFileStatus.Modified
      builder += path.fold(
        GitFile.ModuleFile(path, _, status, timestamp),
        GitFile.ModuleCatalogFile(path, status),
        GitFile.Other(path, status)
      )
    }
    deleted.foreach { path =>
      val status = GitFileStatus.Removed
      builder += path.fold(
        GitFile.ModuleFile(path, _, status, timestamp),
        GitFile.ModuleCatalogFile(path, status),
        GitFile.Other(path, status)
      )
    }
    builder.toList
  }

  private def filesToDownload(files: List[GitFile]): List[GitFile.ModuleFile] =
    files.collect { case module: GitFile.ModuleFile if !module.status.isRemoved => module }

  private def downloadGitFiles(
      branch: Branch,
      moduleFiles: List[GitFile.ModuleFile]
  ): Future[List[(GitFile.ModuleFile, GitFileContent)]] =
    Future.sequence(moduleFiles.map { file =>
      val downloaded = for {
        content      <- downloadService.downloadFileContent(file.path, branch)
        lastModified <- commitService.getCommitDate(file.path, gitConfig.draftBranch)
      } yield (content, lastModified)
      downloaded.collect {
        case (Some(content), Some(lastModified)) => (file.copy(lastModified = lastModified), content)
        case (Some(content), None)               => (file, content)
      }
    })

  override def receive: Receive = {
    case HandleEvent(json, incomingCorrelationId) =>
      given CorrelationId = incomingCorrelationId
      parse(json) match {
        case JsSuccess((branch, gitChanges), _) =>
          if (!branch.isMainBranch) {
            logger.infoC(s"main push skipped branch=${branch.value} reason=not_main_branch")
          } else {
            val moduleFiles = filesToDownload(gitChanges.entries)
            if (moduleFiles.isEmpty) {
              logger.infoC(s"main push skipped branch=${branch.value} reason=empty_changes")
            } else {
              downloadGitFiles(branch, moduleFiles).onComplete {
                case Success(moduleFiles) =>
                  modulePublisher ! ModulePublisher.NotifySubscribers(moduleFiles, incomingCorrelationId)
                  logger.infoC(
                    s"main push ok branch=${branch.value} moduleFiles=${moduleFiles.size}"
                  )
                case Failure(e) =>
                  logger.errorC(s"main push failed branch=${branch.value}", e)
              }
            }
          }
        case JsError(errors) =>
          logger.warnC("main push skipped reason=invalid_event_payload")
          logUnhandedEvent(logger, errors)
      }
  }
}
