package webhook

import java.time.LocalDateTime
import javax.inject.Singleton

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import git._
import git.api.GitFileDownloadService
import git.publisher.CoreDataPublisher
import git.publisher.ModulePublisher
import ops.LoggerOps
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.Props
import play.api.libs.json._
import play.api.Logging

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

  def parse(json: JsValue)(implicit gitConfig: GitConfig) =
    for {
      branch      <- parseBranch(json)
      afterCommit <- parseCommit(json, "after")
      _           <- parseCommit(json, "before")
      (added, modified, deleted, timestamp) <- parseFilesOfLastCommit(
        json,
        afterCommit
      )
    } yield (
      branch,
      GitChanges(toGitFiles(added, modified, deleted), afterCommit, timestamp)
    )

  def toGitFiles(
      added: List[GitFilePath],
      modified: List[GitFilePath],
      deleted: List[GitFilePath]
  )(implicit gitConfig: GitConfig): List[GitFile] = {
    val builder = ListBuffer.empty[GitFile]
    added.foreach { path =>
      val status = GitFileStatus.Added
      builder += path.fold(
        GitFile.ModuleFile(path, _, status),
        GitFile.CoreFile(path, status),
        GitFile.ModuleCatalogFile(path, status),
        GitFile.Other(path, status)
      )
    }
    modified.foreach { path =>
      val status = GitFileStatus.Modified
      builder += path.fold(
        GitFile.ModuleFile(path, _, status),
        GitFile.CoreFile(path, status),
        GitFile.ModuleCatalogFile(path, status),
        GitFile.Other(path, status)
      )
    }
    deleted.foreach { path =>
      val status = GitFileStatus.Removed
      builder += path.fold(
        GitFile.ModuleFile(path, _, status),
        GitFile.CoreFile(path, status),
        GitFile.ModuleCatalogFile(path, status),
        GitFile.Other(path, status)
      )
    }
    builder.toList
  }

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

    override def receive: Receive = {
      case HandleEvent(json) =>
        logger.info("start handling git push event")
        parse(json) match {
          case JsSuccess((branch, gitChanges), _) =>
            if (!branch.isMainBranch) {
              logger.info(
                s"can't handle action on branch ${branch.value}"
              )
            } else {
              val (moduleFiles, coreFiles) = filesToDownload(gitChanges.entries)
              if (moduleFiles.isEmpty && coreFiles.isEmpty) {
                logger.info("can't handle empty changes")
              } else {
                downloadGitFiles(branch, moduleFiles, coreFiles).onComplete {
                  case Success((moduleFiles, coreFiles)) =>
                    modulePublisher.notifySubscribers(
                      moduleFiles,
                      gitChanges.timestamp
                    )
                    coreDataPublisher.notifySubscribers(
                      coreFiles
                    )
                    logger.info("finished!")
                  case Failure(e) =>
                    logFailure(e)
                }
              }
            }
          case JsError(errors) =>
            logUnhandedEvent(logger, errors)
        }
    }

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
        moduleFiles.map(file =>
          downloadService
            .downloadFileContent(file.path, branch)
            .collect { case Some(content) => (file, content) }
        )
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
  }
}
