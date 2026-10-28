package webhook

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.duration.DurationDouble
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Success

import database.repo.ModuleDraftRepository
import database.repo.ModuleRepository
import database.repo.ModuleReviewRepository
import git.*
import git.api.GitBranchService
import git.api.GitCommitService
import git.api.GitFileService
import git.api.GitMergeRequestService
import io.circe.ParsingFailure
import logging.errorC
import logging.infoC
import logging.warnC
import logging.CorrelationId
import models.*
import org.apache.pekko.actor.Actor
import parser.ParsingError
import parsing.yaml.YamlParsingError
import parsing.RawModuleParser
import play.api.libs.json.*
import play.api.Logging
import service.notification.ReviewRejectionNotifier
import service.pipeline.MetadataPipeline
import service.pipeline.Print
import service.ModuleCreationService
import service.SoftBigBangService

final class MergeEventHandler @Inject() (
    repositoryConfig: GitRepositoryConfig,
    reviewConfig: GitReviewConfig,
    moduleReviewRepository: ModuleReviewRepository,
    moduleDraftRepository: ModuleDraftRepository,
    moduleCreationService: ModuleCreationService,
    mergeRequestApiService: GitMergeRequestService,
    branchService: GitBranchService,
    moduleRepository: ModuleRepository,
    gitCommitService: GitCommitService,
    fileService: GitFileService,
    reviewRejectionNotifier: ReviewRejectionNotifier,
    modulePipeline: MetadataPipeline,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  private final case class ParsedMergeEvent(
      mrId: MergeRequestId,
      action: String,
      sourceBranch: Branch,
      targetBranch: Branch,
      labels: Seq[String]
  )

  /**
   * Merges a module draft MR or a soft big bang MR.
   *
   * @param moduleId
   *   the module of a draft MR, whose merge request status is updated after merging. `None` for a soft big bang, as
   *   its MR spans several modules and has no draft.
   * @param sha
   *   the commit of the source branch that passed the check. GitLab only merges if the source branch still points to
   *   it, so commits pushed after the check are never merged unchecked. Set for a soft big bang; `None` for draft MRs,
   *   which are merged in their current state.
   */
  private case class MergeModule(
      correlationId: CorrelationId,
      mrId: MergeRequestId,
      moduleId: Option[UUID],
      sha: Option[CommitId]
  )

  private case class CheckMrStatus(
      correlationId: CorrelationId,
      mrId: MergeRequestId,
      moduleId: Option[UUID],
      attempt: Int,
      sha: Option[CommitId]
  )

  private object MergeRetryPolicy {
    val maxAttempts: Int = 10

    def delayFor(attempt: Int) = Math.pow(2, attempt).seconds + 3.seconds
  }

  private val draftMergeLabels =
    Set(reviewConfig.autoApprovedLabel, reviewConfig.reviewRequiredLabel, reviewConfig.reviewBypassLabel)

  override def receive: Receive = {
    case HandleEvent(json, correlationId) =>
      given CorrelationId = correlationId
      parse(json) match {
        case JsSuccess(event, _) =>
          given ParsedMergeEvent     = event
          def handled(body: => Unit) = {
            logEvent(event)
            body
          }
          (event.sourceBranch, event.targetBranch, event.action) match {
            // Case 1: opened MR from $module_branch into preview branch [AUTO APPROVED]
            // => schedule merge
            case (moduleBranch, repositoryConfig.previewBranch, "open")
                if event.labels.contains(reviewConfig.autoApprovedLabel) =>
              handled(scheduleFreshMerge(moduleBranch))

            // Case 2: merged MR from $module_branch into preview branch [AUTO APPROVED, REVIEW REQUIRED or FAST FORWARD]
            // => delete module draft, update permissions and create module if it's new
            case (moduleBranch, repositoryConfig.previewBranch, "merge") if event.labels.exists(draftMergeLabels) =>
              handled(withUUID(moduleBranch)(moduleId => handleModuleMerged(moduleId, parseMergeCommitSha(json))))

            // Case 3: merged MR from any branch into preview branch
            // => for each module, update permissions and create module if it's new
            case (_, repositoryConfig.previewBranch, "merge") =>
              handled(handleModuleBulkUpdate(parseMergeCommitSha(json)))

            // Case 4: closed MR from $module_branch into preview branch [REVIEW REQUIRED]
            // => handle review reject
            case (moduleBranch, repositoryConfig.previewBranch, "close")
                if event.labels.contains(reviewConfig.reviewRequiredLabel) =>
              handled(withUUID(moduleBranch)(handleReviewReject))

            // Case 5: approved MR from $module_branch into preview branch [REVIEW REQUIRED]
            // => schedule merge
            case (moduleBranch, repositoryConfig.previewBranch, "approved")
                if event.labels.contains(reviewConfig.reviewRequiredLabel) =>
              handled(scheduleFreshMerge(moduleBranch))

            // Case 6: opened MR from sbb branch into main branch [SBB]
            // => type-check all modules in this MR, then merge it or close it and delete its branch
            case (SoftBigBangService.branch, repositoryConfig.mainBranch, "open")
                if event.labels.contains(SoftBigBangService.label) =>
              handled(checkSoftBigBang())

            // Case 7: opened MR from any branch into any branch without labels
            // => type-check all modules in this MR and comment with its results
            case (_, _, "open") if event.labels.isEmpty =>
              handled(logOnFailure(s"module type check failed mr=${event.mrId.value}") {
                typeCheckModules(event.sourceBranch)(using correlationId, event.mrId)
              })

            // unknown action => skip
            case _ =>
              logSkipped(event)
          }
        case JsError(errors) =>
          logUnhandedEvent(logger, errors)
      }

    // Merge MR and update merge request status of the module. For modules, this will eventually trigger case 2
    case MergeModule(correlationId, mrId, moduleId, sha) =>
      given CorrelationId = correlationId
      logOnFailure(s"merge request failed mr=${mrId.value} module=${moduleId.getOrElse("-")}") {
        for {
          mrStatus <- mergeRequestApiService.merge(mrId, sha)
          _        <- moduleId.fold(Future.unit)(moduleDraftRepository.updateMergeRequestStatus(_, mrStatus))
        } yield logger.infoC(s"merge request ok mr=${mrId.value} module=${moduleId.getOrElse("-")}")
      }

    // Check if the merge request is mergeable. If so, merge it. Otherwise, schedule a new attempt with logarithmic backoff
    case CheckMrStatus(correlationId, mrId, moduleId, attempt, sha) =>
      given CorrelationId = correlationId
      if (attempt < MergeRetryPolicy.maxAttempts) {
        mergeRequestApiService.get(mrId).onComplete {
          case Success((_, json)) =>
            val detailedMergeStatus = json.\("detailed_merge_status").validate[String].get
            val mergeStatus         = json.\("merge_status").validate[String].get
            if (detailedMergeStatus == "mergeable" && mergeStatus == "can_be_merged") {
              self ! MergeModule(correlationId, mrId, moduleId, sha)
            } else {
              val nextAttempt = attempt + 1
              val delay       = MergeRetryPolicy.delayFor(nextAttempt)
              logger.infoC(
                s"merge not ready mr=${mrId.value} attempt=$attempt status=$mergeStatus/$detailedMergeStatus retryIn=$delay"
              )
              scheduleMerge(moduleId, nextAttempt, sha)(using correlationId, mrId)
            }
          case Failure(e) =>
            logger.errorC(s"merge status check failed mr=${mrId.value} attempt=$attempt", e)
        }
      } else {
        logger.warnC(
          s"merge status check skipped mr=${mrId.value} attempt=$attempt maxAttempts=${MergeRetryPolicy.maxAttempts} reason=max_attempts_reached"
        )
      }
  }

  private def logEvent(event: ParsedMergeEvent)(using CorrelationId): Unit =
    logger.infoC(
      s"merge event mr=${event.mrId.value} action=${event.action} ${event.sourceBranch.value}->${event.targetBranch.value} labels=${event.labels.mkString(",")}"
    )

  private def logSkipped(event: ParsedMergeEvent)(using CorrelationId): Unit =
    logger.warnC(
      s"merge skipped mr=${event.mrId.value} action=${event.action} ${event.sourceBranch.value}->${event.targetBranch.value} labels=${event.labels.mkString(",")} reason=unsupported_event_shape"
    )

  private def logOnFailure[A](msg: => String)(f: Future[A])(using CorrelationId): Unit =
    f.onComplete {
      case Failure(e) => logger.errorC(msg, e)
      case _          => ()
    }

  private def scheduleFreshMerge(
      moduleBranch: Branch
  )(using correlationId: CorrelationId, event: ParsedMergeEvent): Unit =
    withUUID(moduleBranch)(moduleId => scheduleMerge(Some(moduleId), 0)(using correlationId, event.mrId))

  /** Keeps the checked SBB revision across retries; module draft merges do not require a revision. */
  private def scheduleMerge(
      moduleId: Option[UUID],
      attempt: Int,
      sha: Option[CommitId] = None
  )(using correlationId: CorrelationId, mrId: MergeRequestId) =
    context.system.scheduler.scheduleOnce(
      MergeRetryPolicy.delayFor(attempt),
      self,
      CheckMrStatus(correlationId, mrId, moduleId, attempt, sha)
    )

  private def withUUID(branch: Branch)(k: UUID => Unit)(using CorrelationId, ParsedMergeEvent): Unit =
    try k(UUID.fromString(branch.value))
    catch {
      case NonFatal(_) =>
        logger.warnC(
          s"merge skipped mr=${summon[ParsedMergeEvent].mrId.value} branch=${branch.value} reason=source_branch_not_uuid"
        )
    }

  private def parseMergeCommitSha(json: JsValue): String =
    json.\("object_attributes").\("merge_commit_sha").validate[String].get

  private def parse(json: JsValue): JsResult[ParsedMergeEvent] = {
    val attrs = json.\("object_attributes")
    for {
      mrId         <- attrs.\("iid").validate[Int].map(MergeRequestId.apply)
      action       <- attrs.\("action").validate[String]
      sourceBranch <- attrs.\("source_branch").validate[String].map(Branch.apply)
      targetBranch <- attrs.\("target_branch").validate[String].map(Branch.apply)
      labels       <- attrs
        .\("labels")
        .validate[JsArray]
        .map(_.value.flatMap(l => (l \ "title").asOpt[String]).toSeq)
    } yield ParsedMergeEvent(mrId, action, sourceBranch, targetBranch, labels)
  }

  private def deleteModuleDraft(moduleId: UUID)(using CorrelationId) =
    for
      res1 <- moduleReviewRepository.delete(moduleId)
      res2 <- moduleDraftRepository.delete(moduleId)
    yield logger.infoC(s"module draft deleted after merge module=$moduleId reviews=$res1 drafts=$res2")

  private def handleModuleMerged(moduleId: UUID, sha: String)(using CorrelationId): Unit =
    logOnFailure(s"module merge apply failed module=$moduleId") {
      for {
        (module, diff) <- gitCommitService
          .getLatestModuleFromCommit(sha, repositoryConfig.previewBranch, moduleId)
          .collect {
            case Some((content, diff)) => (parseCreatedModuleInformation(content, moduleId), diff)
          }
        _ <- syncModuleFromMerge(module, diff)
        _ <- deleteModuleDraft(moduleId)
      } yield ()
    }

  // Always sync inherited permissions from git. Upsert created-module only if not yet on main.
  private def syncModuleFromMerge(module: CreatedModule, diff: CommitDiff)(using CorrelationId) =
    for {
      exists <- moduleRepository.exists(module.module)
      _      <-
        if exists then moduleCreationService.syncInheritedPermissions(module)
        else moduleCreationService.createOrUpdateWithPermissions(module)
    } yield {
      val action =
        if exists then "synced_inherited_permissions"
        else if diff.isNewFile then "created_new_module"
        else "updated_module"
      logger.infoC(
        s"module sync from merge module=${module.module} action=$action permissions=${module.moduleManagement.size}"
      )
    }

  private def parseCreatedModuleInformation(content: GitFileContent, module: => UUID) =
    try RawModuleParser.parseCreatedModuleInformation(content.value)
    catch
      case pf: ParsingFailure => throw YamlParsingError(module, pf)
      case pe: ParsingError   => throw YamlParsingError(module, pe)
      case NonFatal(e)        => throw YamlParsingError(module, e)

  private def handleModuleBulkUpdate(sha: String)(using CorrelationId): Unit =
    logOnFailure("module bulk update from merge failed") {
      for
        downloads <- gitCommitService.getAllModulesFromCommit(sha, repositoryConfig.previewBranch)
        _         <- Future.sequence(downloads.map { (content, diff) =>
          val module = parseCreatedModuleInformation(content, diff.newPath.moduleId(repositoryConfig).get)
          syncModuleFromMerge(module, diff)
        })
      yield ()
    }

  /**
   * Type-checks all modules of the MR and comments with its results. Returns whether the check passed. A publication
   * only accepts references to published modules, see [[MetadataPipeline.publicationCheck]]. A commit ref pins both
   * the changed file list and downloads, so they cannot refer to different revisions after a push.
   */
  private def typeCheckModules(ref: Branch | CommitId, publication: Boolean = false)(
      using CorrelationId,
      MergeRequestId
  ): Future[Boolean] = {
    val mrId = summon[MergeRequestId]
    val sha  = ref match {
      case commit: CommitId => Some(commit)
      case _: Branch        => None
    }
    for
      changes   <- mergeRequestApiService.getChanges(mrId, sha)
      downloads <- Future.traverse(changes.map(_.path).filter(_.isModule(repositoryConfig)))(path =>
        fileService.download(path, ref).map(path -> _)
      )
      files   = downloads.flatMap(_._2).map(f => Print(f._1.value))
      deleted = downloads.collect { case (path, None) => path.moduleId(using repositoryConfig) }.flatten.toSet
      ok <-
        if files.isEmpty then
          logger.infoC(s"module type check skipped mr=${mrId.value} reason=no_module_files")
          Future.successful(true)
        else
          (if publication then modulePipeline.publicationCheck(files, deleted)
           else modulePipeline.fullCheck(files)).flatMap {
            case Left(errs) =>
              logger.warnC(s"module type check failed mr=${mrId.value} errorCount=${errs.size}")
              val issues = errs.map(_.getMessage).mkString("\n\n")
              mergeRequestApiService.comment(mrId, s"❌ failed to type check modules.\n\n$issues").map(_ => false)
            case Right(_) =>
              logger.infoC(s"module type check ok mr=${mrId.value}")
              mergeRequestApiService.comment(mrId, "✅ successfully type checked all modules").map(_ => true)
          }
    yield ok
  }

  /**
   * Checks the current SBB commit and schedules a merge of exactly that revision. Invalid modules close the MR and
   * delete its branch; API errors or a changed revision leave it open for inspection.
   */
  private def checkSoftBigBang()(using correlationId: CorrelationId, event: ParsedMergeEvent): Unit = {
    given MergeRequestId = event.mrId
    logOnFailure(s"soft big bang check failed mr=${event.mrId.value}") {
      for {
        sha <- branchService.head(event.sourceBranch)
        ok  <- typeCheckModules(sha, publication = true)
        _   <-
          if ok then Future.successful(scheduleMerge(None, 0, Some(sha)))
          else
            for {
              _ <- mergeRequestApiService.close(event.mrId)
              _ <- branchService.deleteBranch(event.sourceBranch)
            } yield logger.infoC(s"soft big bang closed mr=${event.mrId.value} reason=type_check_failed")
      } yield ()
    }
  }

  private def handleReviewReject(module: UUID)(using CorrelationId): Unit =
    logOnFailure(s"module review rejection notification failed module=$module") {
      reviewRejectionNotifier.notifyIfSingleRejection(module)
    }
}
