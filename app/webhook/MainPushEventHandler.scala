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
import git.publisher.CoreDataPublisher
import git.publisher.ModulePublisher
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.ActorRef
import play.api.libs.json.*
import play.api.Logging

final class MainPushEventHandler @Inject() (
    downloadService: GitFileService,
    commitService: GitCommitService,
    @Named("ModulePublisher") modulePublisher: ActorRef,
    @Named("CoreDataPublisher") coreDataPublisher: ActorRef,
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
        GitFile.CoreFile(path, status),
        GitFile.ModuleCatalogFile(path, status),
        GitFile.Other(path, status)
      )
    }
    modified.foreach { path =>
      val status = GitFileStatus.Modified
      builder += path.fold(
        GitFile.ModuleFile(path, _, status, timestamp),
        GitFile.CoreFile(path, status),
        GitFile.ModuleCatalogFile(path, status),
        GitFile.Other(path, status)
      )
    }
    deleted.foreach { path =>
      val status = GitFileStatus.Removed
      builder += path.fold(
        GitFile.ModuleFile(path, _, status, timestamp),
        GitFile.CoreFile(path, status),
        GitFile.ModuleCatalogFile(path, status),
        GitFile.Other(path, status)
      )
    }
    builder.toList
  }

  // Proceed with created or modified files. Deleted files are ignored
  private def filesToDownload(files: List[GitFile]) = {
    val moduleFiles = ListBuffer.empty[GitFile.ModuleFile]
    val coreFiles   = ListBuffer.empty[GitFile.CoreFile]
    files.foreach {
      case module: GitFile.ModuleFile if !module.status.isRemoved =>
        moduleFiles += module
      case core: GitFile.CoreFile if !core.status.isRemoved =>
        coreFiles += core
      case _ => ()
    }
    (moduleFiles.toList, coreFiles.toList)
  }

  private def downloadGitFiles(
      branch: Branch,
      moduleFiles: List[GitFile.ModuleFile],
      coreFiles: List[GitFile.CoreFile]
  ) = {
    logger.info(s"downloading ${moduleFiles.size + coreFiles.size} files...")
    val downloadedModuleFiles = Future.sequence(
      moduleFiles.map { file =>
        val f = for
          content      <- downloadService.downloadFileContent(file.path, branch)
          lastModified <- commitService.getCommitDate(file.path, gitConfig.draftBranch)
        yield (content, lastModified)
        f.collect {
          case (Some(content), Some(lastModified)) => (file.copy(lastModified = lastModified), content)
          case (Some(content), None)               => (file, content)
        }
      }
    )
    val downloadedCoreFiles = Future.sequence(
      coreFiles.map(file =>
        downloadService
          .downloadFileContent(file.path, branch)
          .collect { case Some(content) => (file, content) }
      )
    )
    for {
      downloadedModuleFiles <- downloadedModuleFiles
      downloadedCoreFiles   <- downloadedCoreFiles
    } yield (downloadedModuleFiles, downloadedCoreFiles)
  }

  override def receive: Receive = {
    case HandleEvent(json) =>
      logger.info("start handling git push event on main branch")
      parse(json) match {
        case JsSuccess((branch, gitChanges), _) =>
          if (!branch.isMainBranch) {
            logger.info(s"can't handle action on branch ${branch.value}")
          } else {
            val (moduleFiles, coreFiles) = filesToDownload(gitChanges.entries)
            if (moduleFiles.isEmpty && coreFiles.isEmpty) {
              logger.info("can't handle empty changes")
            } else {
              downloadGitFiles(branch, moduleFiles, coreFiles).onComplete {
                case Success((moduleFiles, coreFiles)) =>
                  modulePublisher ! ModulePublisher.NotifySubscribers(moduleFiles)
                  coreDataPublisher ! CoreDataPublisher.Handle(coreFiles)
                  logger.info("finished!")
                case Failure(e) =>
                  logger.error("failed to download git files", e)
              }
            }
          }
        case JsError(errors) =>
          logUnhandedEvent(logger, errors)
      }
  }
}
