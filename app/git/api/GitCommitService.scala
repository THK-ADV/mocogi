package git.api

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.*
import models.core.Identity
import models.JsonParseException
import play.api.http.ContentTypes
import play.api.libs.json.*
import play.api.libs.ws.writeableOf_JsValue
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSResponse
import play.mvc.Http.HeaderNames
import play.mvc.Http.Status
import service.pipeline.Print

@Singleton
final class GitCommitService @Inject() (
    private val ws: WSClient,
    private val fileService: GitFileService,
    implicit val config: GitConfig,
    implicit val ctx: ExecutionContext
) extends GitService {

  /**
   * Creates a new commit in a given branch with a specified author, message, and changes to a module.
   */
  def commit(
      branch: Branch,
      author: Identity.Person,
      message: String,
      moduleId: UUID,
      print: Print
  ): Future[CommitId] = {
    val filePath = GitFilePath(moduleId)(config)
    val action   = fileService
      .fileExists(filePath, branch)
      .map(exists =>
        GitCommitAction(
          if (exists) GitCommitActionType.Update
          else GitCommitActionType.Create,
          filePath,
          print.value
        )
      )
    for {
      action <- action
      res    <- commit(
        branch,
        author.email.getOrElse(config.defaultEmail),
        author.fullName,
        message,
        Seq(action)
      )
    } yield res
  }

  /**
   * Returns the content for a module that has been modified in the given commit. This method assumes that there are
   * only module changes and a single diff.
   */
  def getLatestModuleFromCommit(
      sha: String,
      branch: Branch,
      module: UUID
  ): Future[Option[(GitFileContent, CommitDiff)]] =
    getCommitDiff(sha).map(_.collectFirst { case d if d.newPath.moduleId.contains(module) => d }).flatMap {
      case Some(c) => fileService.download(c.newPath, branch).map(_.map(_._1 -> c))
      case None    => Future.successful(None)
    }

  /**
   * Returns the content for all modules that has been modified in the given commit.
   * Deleted Files are not handled
   */
  def getAllModulesFromCommit(sha: String, branch: Branch): Future[List[(GitFileContent, CommitDiff)]] =
    for
      commits   <- getCommitDiff(sha)
      downloads <- Future.sequence(commits.collect {
        case cd if cd.newPath.isModule && !cd.isDeleted =>
          fileService.download(cd.newPath, branch).collect { case Some((c, _)) => (c, cd) }
      })
    yield downloads

  /**
   * Returns the last commit date of a path in a branch
   */
  def getLatestCommitDateOfModulesFolder(): Future[Option[LocalDateTime]] =
    getCommitDate(GitFilePath(config.modulesFolder), config.mainBranch)

  /**
   * Retrieves the date and time of the most recent commit for a specific file path in a given branch.
   */
  def getCommitDate(path: GitFilePath, branch: Branch): Future[Option[LocalDateTime]] =
    ws.url(this.commitUrl())
      .withQueryStringParameters("path" -> path.value, "ref_name" -> branch.value, "per_page" -> "1")
      .withHttpHeaders(tokenHeader(), contentTypeJson())
      .get()
      .flatMap { resp =>
        if resp.status == Status.OK then
          Future.successful(
            resp.json
              .validate[JsArray]
              .flatMap(_.head.\("committed_date").validate[LocalDateTime])
              .asOpt
          )
        else Future.failed(parseErrorMessage(resp))
      }

  /**
   * Retrieves the date and time of a given commit SHA.
   */
  def getCommitDate(sha: String): Future[LocalDateTime] =
    ws.url(s"${this.commitUrl()}/$sha").withHttpHeaders(tokenHeader(), contentTypeJson()).get().flatMap { resp =>
      if resp.status == Status.OK then
        resp.json
          .\("committed_date")
          .validate[LocalDateTime]
          .fold(a => Future.failed(JsonParseException(a)), Future.successful)
      else Future.failed(parseErrorMessage(resp))
    }

  private def commit(
      branch: Branch,
      authorEmail: String,
      authorName: String,
      message: String,
      actions: Seq[GitCommitAction]
  ): Future[CommitId] = {
    def body(): JsValue =
      Json.obj(
        "branch"         -> branch.value,
        "commit_message" -> message,
        "author_email"   -> authorEmail,
        "author_name"    -> authorName,
        "actions"        -> actions.map { a =>
          a.action match {
            case GitCommitActionType.Create =>
              Json.obj(
                "action"    -> a.action.toString,
                "file_path" -> a.filePath.value,
                "content"   -> a.fileContent
              )
            case GitCommitActionType.Delete =>
              Json.obj(
                "action"    -> a.action.toString,
                "file_path" -> a.filePath.value
              )
            case GitCommitActionType.Update =>
              Json.obj(
                "action"    -> a.action.toString,
                "file_path" -> a.filePath.value,
                "content"   -> a.fileContent
              )
          }
        }
      )

    ws
      .url(this.commitUrl())
      .withHttpHeaders(tokenHeader(), contentTypeJson())
      .post(body())
      .flatMap(parseCommitResult)
  }

  private def getCommitDiff(sha: String): Future[List[CommitDiff]] = {
    def parseJson(js: JsValue): List[CommitDiff] =
      js.validate[List[CommitDiff]].fold(_ => List.empty, identity)

    def go(url: String): Future[List[CommitDiff]] =
      ws.url(url)
        .withHttpHeaders(tokenHeader(), contentTypeJson())
        .get()
        .flatMap { resp =>
          if resp.status != Status.OK then Future.failed(parseErrorMessage(resp))
          else {
            val commits = parseJson(resp.json)
            parseNextPaginationUrl(resp) match
              case Some(nextUrl) => go(nextUrl).map(_ ::: commits)
              case None          => Future.successful(commits)
          }
        }

    go(s"${this.commitUrl()}/$sha/diff")
  }

  private def parseCommitResult(res: WSResponse) =
    if (res.status == Status.CREATED)
      Future.successful(
        res.json.\("id").validate[String].map(CommitId.apply).get
      )
    else Future.failed(parseErrorMessage(res))

  private def commitUrl() =
    s"${repositoryUrl()}/commits"

  private def contentTypeJson() =
    (HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
}
