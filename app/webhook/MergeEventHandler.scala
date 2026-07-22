package webhook

import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

import scala.collection.IndexedSeq
import scala.concurrent.duration.DurationDouble
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Success

import cats.data.NonEmptyList
import database.repo.ModuleDraftRepository
import database.repo.ModuleRepository
import database.repo.ModuleReviewRepository
import database.repo.ModuleUpdatePermissionRepository
import git.*
import git.api.GitCommitService
import git.api.GitFileService
import git.api.GitMergeRequestService
import io.circe.ParsingFailure
import logging.AppEventLogger
import logging.CorrelationId
import logging.LogEvent
import logging.LogResult
import models.*
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.ActorRef
import parser.ParsingError
import parsing.yaml.YamlParsingError
import parsing.RawModuleParser
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.libs.json.*
import play.api.Logging
import service.mail.MailActor
import service.mail.MailActor.SendMail
import service.pipeline.MetadataPipeline
import settings.AppSettings
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
    moduleUpdatePermissionRepository: ModuleUpdatePermissionRepository,
    messages: MessagesApi,
    fileService: GitFileService,
    @Named("MailActor") mailActor: ActorRef,
    appSettings: AppSettings,
    modulePipeline: MetadataPipeline,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  private def moduleEditUrl: String = appSettings.mail.editUrl

  private type Action = String
  private type Labels = IndexedSeq[String]

  private final case class ParsedMergeEvent(
      mrId: MergeRequestId,
      action: Action,
      sourceBranch: Branch,
      targetBranch: Branch,
      labels: Labels
  )

  given Lang(Locale.GERMANY)

  private object MergeRetryPolicy {
    val maxAttempts: Int = 10

    def delayFor(attempt: Int) =
      Math.pow(2, attempt).seconds + 3.seconds
  }

  override def receive: Receive = {
    case HandleEvent(json, incomingCorrelationId) =>
      implicit val id: CorrelationId = incomingCorrelationId
      parse(json) match {
        case JsSuccess(parsedEvent, _) =>
          implicit val result: ParsedMergeEvent = parsedEvent
          implicit val mrId: MergeRequestId     = parsedEvent.mrId
          val action                            = parsedEvent.action
          val sourceBranch                      = parsedEvent.sourceBranch
          val targetBranch                      = parsedEvent.targetBranch
          val labels                            = parsedEvent.labels
          (sourceBranch, targetBranch, action) match {
            // Case 1: opened MR from $module_branch into draft branch [AUTO APPROVED]
            // => schedule merge
            case (moduleBranch, gitConfig.draftBranch, "open") if labels.contains(gitConfig.autoApprovedLabel) =>
              logEvent(action, sourceBranch, targetBranch, labels)
              scheduleFreshMerge(moduleBranch)

            // Case 2: merged MR from $module_branch into draft branch [AUTO APPROVED or REVIEW REQUIRED]
            // => delete module draft, update permissions and create module if it's new
            case (moduleBranch, gitConfig.draftBranch, "merge")
                if labels.contains(gitConfig.autoApprovedLabel) || labels.contains(gitConfig.reviewRequiredLabel) || labels.contains(gitConfig.fastForwardLabel) =>
              logEvent(action, sourceBranch, targetBranch, labels)
              val sha = parseMergeCommitSha(json)
              withUUID(moduleBranch)(moduleId => handleModuleCreated(id, moduleId, sha))

            // Case 3: merged MR from any branch into draft branch
            // => for each module, update permissions and create module if it's new
            case (_, gitConfig.draftBranch, "merge") =>
              logEvent(action, sourceBranch, targetBranch, labels)
              val sha = parseMergeCommitSha(json)
              handleModuleBulkUpdate(id, sha)

            // Case 4: closed MR from $module_branch into draft branch [REVIEW REQUIRED]
            // => handle review reject
            case (moduleBranch, gitConfig.draftBranch, "close") if labels.contains(gitConfig.reviewRequiredLabel) =>
              logEvent(action, sourceBranch, targetBranch, labels)
              withUUID(moduleBranch)(moduleId => handleReviewReject(moduleId))

            // Case 5: approved MR from $module_branch into draft branch [REVIEW REQUIRED]
            // => schedule merge
            case (moduleBranch, gitConfig.draftBranch, "approved") if labels.contains(gitConfig.reviewRequiredLabel) =>
              logEvent(action, sourceBranch, targetBranch, labels)
              scheduleFreshMerge(moduleBranch)

            // Case 6: opened MR from any branch into any branch without labels
            // => type-check all modules in this MR and comment with its results
            case (_, _, "open") if labels.isEmpty =>
              logEvent(action, sourceBranch, targetBranch, labels)
              typeCheckModules(sourceBranch)

            // unknown action => abort
            case _ =>
              abort(id, result)
          }
        case JsError(errors) =>
          logUnhandedEvent(logger, errors)
          self ! Finished(id)
      }

    // Merge module MR and update merge request status of the module. This will eventually trigger case 2
    case MergeModule(id, mrID, moduleId) =>
      val event = "git.merge.request"
      infoEvent(
        event = event,
        result = LogResult.Started,
        moduleId = Some(moduleId),
        mrId = Some(mrID),
        details = Map("action" -> "merge")
      )(id)
      val f = for {
        mrStatus <- mergeRequestApiService.merge(mrID)
        _        <- moduleDraftRepository.updateMergeRequestStatus(moduleId, mrStatus)
      } yield infoEvent(
        event = event,
        result = LogResult.Succeeded,
        moduleId = Some(moduleId),
        mrId = Some(mrID),
        details = Map("action" -> "merge")
      )(id)
      f.onComplete {
        case Success(_) =>
          self ! Finished(id)
        case Failure(e) =>
          errorEvent(
            event = event,
            result = LogResult.Failed,
            throwable = e,
            moduleId = Some(moduleId),
            mrId = Some(mrID),
            errorCode = Some("merge_request_failed")
          )(id)
          self ! Finished(id)
      }

    // Check if the merge request is mergeable. If so, merge the module (case 2). Otherwise, schedule a new attempt with logarithmic backoff
    case CheckMrStatus(id, mrId, attempt, merge) =>
      val event = "git.merge.check_status"
      if (attempt < MergeRetryPolicy.maxAttempts) {
        infoEvent(
          event = event,
          result = LogResult.Started,
          mrId = Some(mrId),
          details = Map("attempt" -> attempt.toString)
        )(id)
        mergeRequestApiService.get(mrId).onComplete {
          case Success((_, json)) =>
            val detailedMergeStatus = json.\("detailed_merge_status").validate[String].get
            val mergeStatus         = json.\("merge_status").validate[String].get
            infoEvent(
              event = event,
              result = LogResult.Succeeded,
              mrId = Some(mrId),
              details = Map(
                "attempt"             -> attempt.toString,
                "mergeStatus"         -> mergeStatus,
                "detailedMergeStatus" -> detailedMergeStatus
              )
            )(id)
            if (detailedMergeStatus == "mergeable" && mergeStatus == "can_be_merged") {
              merge()
            } else {
              scheduleMerge(attempt + 1, merge)(id, mrId)
            }
          case Failure(e) =>
            errorEvent(
              event = event,
              result = LogResult.Failed,
              throwable = e,
              mrId = Some(mrId),
              errorCode = Some("merge_status_check_failed"),
              details = Map("attempt" -> attempt.toString)
            )(id)
            self ! Finished(id)
        }
      } else {
        warnEvent(
          event = event,
          result = LogResult.Skipped,
          mrId = Some(mrId),
          details = Map(
            "attempt"     -> attempt.toString,
            "maxAttempts" -> MergeRetryPolicy.maxAttempts.toString,
            "reason"      -> "max_attempts_reached"
          )
        )(id)
        self ! Finished(id)
      }

    case Finished(id) =>
      infoEvent(event = "git.merge.event", result = LogResult.Succeeded)(id)
  }

  private def logEvent(
      action: Action,
      source: Branch,
      target: Branch,
      labels: Labels
  )(implicit id: CorrelationId, mrId: MergeRequestId): Unit =
    infoEvent(
      event = "git.merge.event.received",
      result = LogResult.Started,
      mrId = Some(mrId),
      branch = Some(target),
      details = Map(
        "action"       -> action,
        "sourceBranch" -> source.value,
        "targetBranch" -> target.value,
        "labels"       -> labels.mkString(",")
      )
    )

  private def scheduleFreshMerge(
      moduleBranch: Branch
  )(implicit id: CorrelationId, mrId: MergeRequestId, result: ParsedMergeEvent): Unit =
    withUUID(moduleBranch)(moduleId => scheduleMerge(0, () => self ! MergeModule(id, mrId, moduleId)))

  private case class MergeModule(id: CorrelationId, mrId: MergeRequestId, moduleId: UUID)

  private case class Finished(id: CorrelationId)

  private case class CheckMrStatus(id: CorrelationId, mrId: MergeRequestId, attempt: Int, merge: () => Unit)

  private def scheduleMerge(attempt: Int, merge: () => Unit)(implicit id: CorrelationId, mrId: MergeRequestId) = {
    val delay = MergeRetryPolicy.delayFor(attempt)
    if attempt > 0 then
      infoEvent(
        event = "git.merge.retry_scheduled",
        result = LogResult.Started,
        mrId = Some(mrId),
        details = Map("attempt" -> attempt.toString, "delay" -> delay.toString)
      )
    context.system.scheduler.scheduleOnce(delay, self, CheckMrStatus(id, mrId, attempt, merge))
  }

  private def withUUID(branch: Branch)(k: UUID => Unit)(implicit id: CorrelationId, result: ParsedMergeEvent): Unit =
    try {
      val moduleId = UUID.fromString(branch.value)
      k(moduleId)
    } catch {
      case NonFatal(_) =>
        warnEvent(
          event = "git.merge.event.branch_parse",
          result = LogResult.Skipped,
          mrId = Some(result.mrId),
          details = Map(
            "sourceBranch" -> branch.value,
            "reason"       -> "source_branch_not_uuid"
          )
        )
        abort(id, result)
        self ! Finished(id)
    }

  private def parseMergeCommitSha(json: JsValue): String =
    json.\("object_attributes").\("merge_commit_sha").validate[String].get

  private def parse(json: JsValue): JsResult[ParsedMergeEvent] = {
    val attrs = json.\("object_attributes")
    for {
      mrId         <- attrs.\("iid").validate[Int].map(MergeRequestId.apply)
      action       <- attrs.\("action").validate[String]
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
    } yield ParsedMergeEvent(mrId, action, sourceBranch, targetBranch, labels)
  }

  private def deleteModuleDraft(id: CorrelationId, moduleId: UUID) =
    for
      res1 <- moduleReviewRepository.delete(moduleId)
      res2 <- moduleDraftRepository.delete(moduleId)
    yield infoEvent(
      event = "module.draft.deleted_after_merge",
      result = LogResult.Succeeded,
      moduleId = Some(moduleId),
      details = Map("deletedReviews" -> res1.toString, "deletedDrafts" -> res2.toString)
    )(id)

  // TODO first, the name of the method is irritating. Second, it does not consider updating the module 
  //  (e.g., updating permissions based on module management)
  private def handleModuleCreated(id: CorrelationId, moduleId: UUID, sha: String): Unit = {
    val f = for {
      (module, diff) <- gitCommitService.getLatestModuleFromCommit(sha, gitConfig.draftBranch, moduleId).collect {
        case Some((content, diff)) => (parseCreatedModuleInformation(content, moduleId), diff)
      }
      _ <- createNewModuleWithPermissionsIfNeeded(id, module, diff)
      _ <- deleteModuleDraft(id, moduleId)
    } yield ()
    f.onComplete {
      case Success(_) =>
        self ! Finished(id)
      case Failure(e) =>
        errorEvent(
          event = "module.merge.apply",
          result = LogResult.Failed,
          throwable = e,
          moduleId = Some(moduleId),
          errorCode = Some("module_merge_apply_failed")
        )(id)
        self ! Finished(id)
    }
  }

  private def createNewModuleWithPermissionsIfNeeded(id: CorrelationId, module: CreatedModule, diff: CommitDiff) =
    for {
      exists <- moduleRepository.exists(module.module)
      res    <-
        if exists then Future.unit // it's not a new module if it already exists
        else
          moduleCreationService.createOrUpdateWithPermissions(module).map { _ =>
            val prefixStr = if diff.isNewFile then "created new module" else "updated module"
            infoEvent(
              event = "module.sync_from_merge",
              result = LogResult.Succeeded,
              moduleId = Some(module.module),
              details = Map(
                "action"           -> prefixStr.replace(" ", "_"),
                "permissionsCount" -> module.moduleManagement.size.toString
              )
            )(id)
          }
    } yield res

  private def parseCreatedModuleInformation(content: GitFileContent, module: => UUID) =
    try RawModuleParser.parseCreatedModuleInformation(content.value)
    catch
      case pf: ParsingFailure => throw YamlParsingError(module, pf)
      case pe: ParsingError   => throw YamlParsingError(module, pe)
      case NonFatal(e)        => throw YamlParsingError(module, e)

  private def handleModuleBulkUpdate(id: CorrelationId, sha: String): Unit = {
    val f = for
      downloads <- gitCommitService.getAllModulesFromCommit(sha, gitConfig.draftBranch)
      _         <- Future.sequence(downloads.map { (content, diff) =>
        val module = parseCreatedModuleInformation(content, diff.newPath.moduleId(gitConfig).get)
        createNewModuleWithPermissionsIfNeeded(id, module, diff)
      })
    yield ()

    f.onComplete {
      case Success(_) =>
        self ! Finished(id)
      case Failure(e) =>
        errorEvent(
          event = "module.bulk_update_from_merge",
          result = LogResult.Failed,
          throwable = e,
          errorCode = Some("bulk_update_failed")
        )(id)
        self ! Finished(id)
    }
  }

  private def typeCheckModules(branch: Branch)(implicit id: CorrelationId, mrId: MergeRequestId): Unit = {
    val event = "module.type_check"
    infoEvent(
      event = event,
      result = LogResult.Started,
      mrId = Some(mrId),
      branch = Some(branch)
    )
    val f = for
      changes   <- mergeRequestApiService.getChanges(mrId)
      downloads <- Future.sequence(changes.collect {
        case d if d.path.isModule(gitConfig) => fileService.download(d.path, branch)
      })
      _ <-
        if downloads.isEmpty then
          Future.successful(
            infoEvent(
              event = event,
              result = LogResult.Skipped,
              mrId = Some(mrId),
              details = Map("reason" -> "no_module_files")
            )
          )
        else {
          for {
            parseRes <- modulePipeline.parseValidateMany(downloads.collect { case Some(f) => Print(f._1.value) })
            _        <- parseRes match {
              case Left(errs) =>
                warnEvent(
                  event = event,
                  result = LogResult.Failed,
                  mrId = Some(mrId),
                  details = Map("errorCount" -> errs.size.toString)
                )
                val comments = errs.map { err =>
                  val body =
                    s"❌ failed to type check module ${err.metadata.fold("???")(_.toString)}.\n\nreason:${err.getMessage}"
                  mergeRequestApiService.comment(mrId, body)
                }
                Future.sequence(comments)
              case Right(_) =>
                mergeRequestApiService
                  .comment(mrId, "✅ successfully type checked all modules")
                  .map(_ =>
                    infoEvent(
                      event = event,
                      result = LogResult.Succeeded,
                      mrId = Some(mrId)
                    )
                  )
            }
          } yield ()
        }
    yield ()

    f.onComplete {
      case Success(_) =>
        self ! Finished(id)
      case Failure(e) =>
        errorEvent(
          event = event,
          result = LogResult.Failed,
          throwable = e,
          mrId = Some(mrId),
          errorCode = Some("module_type_check_failed")
        )(id)
        self ! Finished(id)
    }
  }

  // TODO: class MailComposer?
  private def handleReviewReject(module: UUID)(using id: CorrelationId): Unit = {
    val reviewEvent                                   = "module.review.rejected"
    val notificationEvent                             = "module.review.rejected_notification"
    def sendMail(rejectedReview: ModuleReview.Atomic) = {
      for
        moduleTitle <- moduleDraftRepository.getModuleTitle(module)
        users       <- moduleUpdatePermissionRepository.allPeopleWithPermissionForModule(module)
      yield {
        infoEvent(
          event = reviewEvent,
          result = LogResult.Succeeded,
          moduleId = Some(module)
        )(id)
        val sb = new StringBuilder()
        sb.append(
          messages(
            "module_review.rejection.notification.opening",
            moduleTitle,
            rejectedReview.respondedBy.fold("???")(_.fullName),
            moduleEditUrl.replace("$moduleid", module.toString)
          )
        )
        rejectedReview.comment.foreach { comment =>
          sb.append("\n\n")
          val quoted = s"\n${comment.trim}".replaceAll("\n", "\n>")
          sb.append(messages("module_review.rejection.notification.reason", quoted))
        }
        sb.append("\n\n")
        sb.append(messages("module_review.rejection.notification.closing"))

        val to = users.collect { case (person, perm) if perm.isInherited && person.hasEmail => person.email.get }
        val cc = users.collect { case (person, perm) if perm.isGranted && person.hasEmail => person.email.get }

        NonEmptyList.fromList(to.toList) match
          case Some(to) =>
            mailActor ! SendMail(
              messages("module_review.rejection.notification.subject", moduleTitle),
              sb.toString(),
              to,
              cc.toList
            )
          case None =>
            warnEvent(
              event = notificationEvent,
              result = LogResult.Skipped,
              moduleId = Some(module),
              details = Map("reason" -> "missing_inherited_permissions_recipient")
            )
      }
    }
    val f = for
      reviews <- moduleReviewRepository.getAtomicByModule(module)
      _       <- {
        val rejected = reviews.filter(_.status.isRejected)
        if rejected.size == 1 then sendMail(rejected.head) else Future.unit
      }
    yield ()

    f.onComplete {
      case Success(_) =>
        self ! Finished(id)
      case Failure(e) =>
        errorEvent(
          event = notificationEvent,
          result = LogResult.Failed,
          throwable = e,
          moduleId = Some(module),
          errorCode = Some("review_rejection_notification_failed")
        )(id)
        self ! Finished(id)
    }
  }

  private def abort(id: CorrelationId, result: ParsedMergeEvent): Unit =
    warnEvent(
      event = "git.merge.event",
      result = LogResult.Skipped,
      mrId = Some(result.mrId),
      branch = Some(result.targetBranch),
      details = Map(
        "action"       -> result.action,
        "sourceBranch" -> result.sourceBranch.value,
        "targetBranch" -> result.targetBranch.value,
        "labels"       -> result.labels.mkString(","),
        "reason"       -> "unsupported_event_shape"
      )
    )(id)

  private def infoEvent(
      event: String,
      result: LogResult,
      moduleId: Option[UUID] = None,
      mrId: Option[MergeRequestId] = None,
      branch: Option[Branch] = None,
      actor: Option[String] = None,
      details: Map[String, String] = Map.empty
  )(implicit correlationId: CorrelationId): Unit =
    AppEventLogger.info(
      logger,
      LogEvent(
        event = event,
        result = result,
        correlationId = correlationId,
        moduleId = moduleId,
        mrId = mrId.map(_.value),
        branch = branch.map(_.value),
        actor = actor,
        details = details
      )
    )

  private def warnEvent(
      event: String,
      result: LogResult,
      moduleId: Option[UUID] = None,
      mrId: Option[MergeRequestId] = None,
      branch: Option[Branch] = None,
      actor: Option[String] = None,
      errorCode: Option[String] = None,
      details: Map[String, String] = Map.empty
  )(implicit correlationId: CorrelationId): Unit =
    AppEventLogger.warn(
      logger,
      LogEvent(
        event = event,
        result = result,
        correlationId = correlationId,
        moduleId = moduleId,
        mrId = mrId.map(_.value),
        branch = branch.map(_.value),
        actor = actor,
        errorCode = errorCode,
        details = details
      )
    )

  private def errorEvent(
      event: String,
      result: LogResult,
      throwable: Throwable,
      moduleId: Option[UUID] = None,
      mrId: Option[MergeRequestId] = None,
      branch: Option[Branch] = None,
      actor: Option[String] = None,
      errorCode: Option[String] = None,
      details: Map[String, String] = Map.empty
  )(implicit correlationId: CorrelationId): Unit =
    AppEventLogger.error(
      logger,
      LogEvent(
        event = event,
        result = result,
        correlationId = correlationId,
        moduleId = moduleId,
        mrId = mrId.map(_.value),
        branch = branch.map(_.value),
        actor = actor,
        errorCode = errorCode,
        details = details
      ),
      throwable
    )
}
