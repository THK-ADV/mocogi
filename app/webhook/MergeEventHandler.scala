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

final class MergeEventHandler @Inject() (
    gitConfig: GitConfig,
    moduleReviewRepository: ModuleReviewRepository,
    moduleDraftRepository: ModuleDraftRepository,
    moduleCreationService: ModuleCreationService,
    mergeRequestApiService: GitMergeRequestService,
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

  private case class MergeModule(correlationId: CorrelationId, mrId: MergeRequestId, moduleId: UUID)

  private case class CheckMrStatus(
      correlationId: CorrelationId,
      mrId: MergeRequestId,
      moduleId: UUID,
      attempt: Int
  )

  private object MergeRetryPolicy {
    val maxAttempts: Int = 10

    def delayFor(attempt: Int) =
      Math.pow(2, attempt).seconds + 3.seconds
  }

  private val draftMergeLabels =
    Set(gitConfig.autoApprovedLabel, gitConfig.reviewRequiredLabel, gitConfig.fastForwardLabel)

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
            // Case 1: opened MR from $module_branch into draft branch [AUTO APPROVED]
            // => schedule merge
            case (moduleBranch, gitConfig.draftBranch, "open") if event.labels.contains(gitConfig.autoApprovedLabel) =>
              handled(scheduleFreshMerge(moduleBranch))

            // Case 2: merged MR from $module_branch into draft branch [AUTO APPROVED, REVIEW REQUIRED or FAST FORWARD]
            // => delete module draft, update permissions and create module if it's new
            case (moduleBranch, gitConfig.draftBranch, "merge") if event.labels.exists(draftMergeLabels) =>
              handled(withUUID(moduleBranch)(moduleId => handleModuleMerged(moduleId, parseMergeCommitSha(json))))

            // Case 3: merged MR from any branch into draft branch
            // => for each module, update permissions and create module if it's new
            case (_, gitConfig.draftBranch, "merge") =>
              handled(handleModuleBulkUpdate(parseMergeCommitSha(json)))

            // Case 4: closed MR from $module_branch into draft branch [REVIEW REQUIRED]
            // => handle review reject
            case (moduleBranch, gitConfig.draftBranch, "close")
                if event.labels.contains(gitConfig.reviewRequiredLabel) =>
              handled(withUUID(moduleBranch)(handleReviewReject))

            // Case 5: approved MR from $module_branch into draft branch [REVIEW REQUIRED]
            // => schedule merge
            case (moduleBranch, gitConfig.draftBranch, "approved")
                if event.labels.contains(gitConfig.reviewRequiredLabel) =>
              handled(scheduleFreshMerge(moduleBranch))

            // Case 6: opened MR from any branch into any branch without labels
            // => type-check all modules in this MR and comment with its results
            case (_, _, "open") if event.labels.isEmpty =>
              handled(typeCheckModules(event.sourceBranch)(using correlationId, event.mrId))

            // unknown action => skip
            case _ =>
              logSkipped(event)
          }
        case JsError(errors) =>
          logUnhandedEvent(logger, errors)
      }

    // Merge module MR and update merge request status of the module. This will eventually trigger case 2
    case MergeModule(correlationId, mrId, moduleId) =>
      given CorrelationId = correlationId
      logOnFailure(s"merge request failed mr=${mrId.value} module=$moduleId") {
        for {
          mrStatus <- mergeRequestApiService.merge(mrId)
          _        <- moduleDraftRepository.updateMergeRequestStatus(moduleId, mrStatus)
        } yield logger.infoC(s"merge request ok mr=${mrId.value} module=$moduleId")
      }

    // Check if the merge request is mergeable. If so, merge the module (case 2). Otherwise, schedule a new attempt with logarithmic backoff
    case CheckMrStatus(correlationId, mrId, moduleId, attempt) =>
      given CorrelationId = correlationId
      if (attempt < MergeRetryPolicy.maxAttempts) {
        mergeRequestApiService.get(mrId).onComplete {
          case Success((_, json)) =>
            val detailedMergeStatus = json.\("detailed_merge_status").validate[String].get
            val mergeStatus         = json.\("merge_status").validate[String].get
            if (detailedMergeStatus == "mergeable" && mergeStatus == "can_be_merged") {
              self ! MergeModule(correlationId, mrId, moduleId)
            } else {
              val nextAttempt = attempt + 1
              val delay       = MergeRetryPolicy.delayFor(nextAttempt)
              logger.infoC(
                s"merge not ready mr=${mrId.value} attempt=$attempt status=$mergeStatus/$detailedMergeStatus retryIn=$delay"
              )
              scheduleMerge(moduleId, nextAttempt)(using correlationId, mrId)
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
    withUUID(moduleBranch)(moduleId => scheduleMerge(moduleId, 0)(using correlationId, event.mrId))

  private def scheduleMerge(moduleId: UUID, attempt: Int)(using correlationId: CorrelationId, mrId: MergeRequestId) =
    context.system.scheduler.scheduleOnce(
      MergeRetryPolicy.delayFor(attempt),
      self,
      CheckMrStatus(correlationId, mrId, moduleId, attempt)
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
        (module, diff) <- gitCommitService.getLatestModuleFromCommit(sha, gitConfig.draftBranch, moduleId).collect {
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
        downloads <- gitCommitService.getAllModulesFromCommit(sha, gitConfig.draftBranch)
        _         <- Future.sequence(downloads.map { (content, diff) =>
          val module = parseCreatedModuleInformation(content, diff.newPath.moduleId(gitConfig).get)
          syncModuleFromMerge(module, diff)
        })
      yield ()
    }

  private def typeCheckModules(branch: Branch)(using CorrelationId, MergeRequestId): Unit = {
    val mrId = summon[MergeRequestId]
    logOnFailure(s"module type check failed mr=${mrId.value}") {
      for
        changes   <- mergeRequestApiService.getChanges(mrId)
        downloads <- Future.sequence(changes.collect {
          case d if d.path.isModule(gitConfig) => fileService.download(d.path, branch)
        })
        files = downloads.flatten
        _ <-
          if files.isEmpty then
            Future.successful(logger.infoC(s"module type check skipped mr=${mrId.value} reason=no_module_files"))
          else
            modulePipeline.parseValidateMany(files.map(f => Print(f._1.value))).flatMap {
              case Left(errs) =>
                logger.warnC(s"module type check failed mr=${mrId.value} errorCount=${errs.size}")
                Future.sequence(errs.map { err =>
                  val body =
                    s"❌ failed to type check module ${err.metadata.fold("???")(_.toString)}.\n\nreason:${err.getMessage}"
                  mergeRequestApiService.comment(mrId, body)
                })
              case Right(_) =>
                mergeRequestApiService
                  .comment(mrId, "✅ successfully type checked all modules")
                  .map(_ => logger.infoC(s"module type check ok mr=${mrId.value}"))
            }
      yield ()
    }
  }

  private def handleReviewReject(module: UUID)(using CorrelationId): Unit =
    logOnFailure(s"module review rejection notification failed module=$module") {
      reviewRejectionNotifier.notifyIfSingleRejection(module)
    }
}
