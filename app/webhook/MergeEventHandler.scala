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
import logging.errorC
import logging.infoC
import logging.warnC
import logging.CorrelationId
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
      given CorrelationId = incomingCorrelationId
      parse(json) match {
        case JsSuccess(parsedEvent, _) =>
          given ParsedMergeEvent = parsedEvent
          given MergeRequestId   = parsedEvent.mrId
          val action             = parsedEvent.action
          val sourceBranch       = parsedEvent.sourceBranch
          val targetBranch       = parsedEvent.targetBranch
          val labels             = parsedEvent.labels
          (sourceBranch, targetBranch, action) match {
            // Case 1: opened MR from $module_branch into draft branch [AUTO APPROVED]
            // => schedule merge
            case (moduleBranch, gitConfig.draftBranch, "open") if labels.contains(gitConfig.autoApprovedLabel) =>
              logEvent(action, sourceBranch, targetBranch, labels)
              scheduleFreshMerge(moduleBranch)

            // Case 2: merged MR from $module_branch into draft branch [AUTO APPROVED or REVIEW REQUIRED]
            // => delete module draft, update permissions and create module if it's new
            case (moduleBranch, gitConfig.draftBranch, "merge")
                if labels.contains(gitConfig.autoApprovedLabel) || labels.contains(
                  gitConfig.reviewRequiredLabel
                ) || labels.contains(gitConfig.fastForwardLabel) =>
              logEvent(action, sourceBranch, targetBranch, labels)
              val sha = parseMergeCommitSha(json)
              withUUID(moduleBranch)(moduleId => handleModuleCreated(incomingCorrelationId, moduleId, sha))

            // Case 3: merged MR from any branch into draft branch
            // => for each module, update permissions and create module if it's new
            case (_, gitConfig.draftBranch, "merge") =>
              logEvent(action, sourceBranch, targetBranch, labels)
              val sha = parseMergeCommitSha(json)
              handleModuleBulkUpdate(incomingCorrelationId, sha)

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
              abort(parsedEvent)
          }
        case JsError(errors) =>
          logUnhandedEvent(logger, errors)
      }

    // Merge module MR and update merge request status of the module. This will eventually trigger case 2
    case MergeModule(id, mrID, moduleId) =>
      given CorrelationId = id
      val f               = for {
        mrStatus <- mergeRequestApiService.merge(mrID)
        _        <- moduleDraftRepository.updateMergeRequestStatus(moduleId, mrStatus)
      } yield logger.infoC(s"merge request ok mr=${mrID.value} module=$moduleId")
      f.onComplete {
        case Failure(e) =>
          logger.errorC(s"merge request failed mr=${mrID.value} module=$moduleId", e)
        case _ => ()
      }

    // Check if the merge request is mergeable. If so, merge the module (case 2). Otherwise, schedule a new attempt with logarithmic backoff
    case CheckMrStatus(id, mrId, attempt, merge) =>
      given CorrelationId = id
      if (attempt < MergeRetryPolicy.maxAttempts) {
        mergeRequestApiService.get(mrId).onComplete {
          case Success((_, json)) =>
            val detailedMergeStatus = json.\("detailed_merge_status").validate[String].get
            val mergeStatus         = json.\("merge_status").validate[String].get
            if (detailedMergeStatus == "mergeable" && mergeStatus == "can_be_merged") {
              merge()
            } else {
              val nextAttempt = attempt + 1
              val delay       = MergeRetryPolicy.delayFor(nextAttempt)
              logger.infoC(
                s"merge not ready mr=${mrId.value} attempt=$attempt status=$mergeStatus/$detailedMergeStatus retryIn=$delay"
              )
              scheduleMerge(nextAttempt, merge)(using id, mrId)
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

  private def logEvent(
      action: Action,
      source: Branch,
      target: Branch,
      labels: Labels
  )(using CorrelationId, MergeRequestId): Unit =
    logger.infoC(
      s"merge event mr=${summon[MergeRequestId].value} action=$action ${source.value}->${target.value} labels=${labels.mkString(",")}"
    )

  private def scheduleFreshMerge(
      moduleBranch: Branch
  )(using id: CorrelationId, mrId: MergeRequestId, result: ParsedMergeEvent): Unit =
    withUUID(moduleBranch)(moduleId => scheduleMerge(0, () => self ! MergeModule(id, mrId, moduleId)))

  private case class MergeModule(id: CorrelationId, mrId: MergeRequestId, moduleId: UUID)

  private case class CheckMrStatus(id: CorrelationId, mrId: MergeRequestId, attempt: Int, merge: () => Unit)

  private def scheduleMerge(attempt: Int, merge: () => Unit)(using id: CorrelationId, mrId: MergeRequestId) =
    context.system.scheduler.scheduleOnce(
      MergeRetryPolicy.delayFor(attempt),
      self,
      CheckMrStatus(id, mrId, attempt, merge)
    )

  private def withUUID(branch: Branch)(k: UUID => Unit)(using CorrelationId, ParsedMergeEvent): Unit =
    try {
      val moduleId = UUID.fromString(branch.value)
      k(moduleId)
    } catch {
      case NonFatal(_) =>
        logger.warnC(
          s"merge skipped mr=${summon[ParsedMergeEvent].mrId.value} branch=${branch.value} reason=source_branch_not_uuid"
        )
        abort(summon[ParsedMergeEvent])
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

  private def deleteModuleDraft(moduleId: UUID)(using CorrelationId) =
    for
      res1 <- moduleReviewRepository.delete(moduleId)
      res2 <- moduleDraftRepository.delete(moduleId)
    yield logger.infoC(s"module draft deleted after merge module=$moduleId reviews=$res1 drafts=$res2")

  private def handleModuleCreated(id: CorrelationId, moduleId: UUID, sha: String): Unit = {
    given CorrelationId = id
    val f               = for {
      (module, diff) <- gitCommitService.getLatestModuleFromCommit(sha, gitConfig.draftBranch, moduleId).collect {
        case Some((content, diff)) => (parseCreatedModuleInformation(content, moduleId), diff)
      }
      _ <- syncModuleFromMerge(module, diff)
      _ <- deleteModuleDraft(moduleId)
    } yield ()
    f.onComplete {
      case Failure(e) =>
        logger.errorC(s"module merge apply failed module=$moduleId", e)
      case _ => ()
    }
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

  private def handleModuleBulkUpdate(id: CorrelationId, sha: String): Unit = {
    given CorrelationId = id
    val f               = for
      downloads <- gitCommitService.getAllModulesFromCommit(sha, gitConfig.draftBranch)
      _         <- Future.sequence(downloads.map { (content, diff) =>
        val module = parseCreatedModuleInformation(content, diff.newPath.moduleId(gitConfig).get)
        syncModuleFromMerge(module, diff)
      })
    yield ()

    f.onComplete {
      case Failure(e) =>
        logger.errorC("module bulk update from merge failed", e)
      case _ => ()
    }
  }

  private def typeCheckModules(branch: Branch)(using id: CorrelationId, mrId: MergeRequestId): Unit = {
    val f = for
      changes   <- mergeRequestApiService.getChanges(mrId)
      downloads <- Future.sequence(changes.collect {
        case d if d.path.isModule(gitConfig) => fileService.download(d.path, branch)
      })
      _ <-
        if downloads.isEmpty then
          Future.successful(
            logger.infoC(s"module type check skipped mr=${mrId.value} reason=no_module_files")
          )
        else {
          for {
            parseRes <- modulePipeline.parseValidateMany(downloads.collect { case Some(f) => Print(f._1.value) })
            _        <- parseRes match {
              case Left(errs) =>
                logger.warnC(s"module type check failed mr=${mrId.value} errorCount=${errs.size}")
                val comments = errs.map { err =>
                  val body =
                    s"❌ failed to type check module ${err.metadata.fold("???")(_.toString)}.\n\nreason:${err.getMessage}"
                  mergeRequestApiService.comment(mrId, body)
                }
                Future.sequence(comments)
              case Right(_) =>
                mergeRequestApiService
                  .comment(mrId, "✅ successfully type checked all modules")
                  .map(_ => logger.infoC(s"module type check ok mr=${mrId.value}"))
            }
          } yield ()
        }
    yield ()

    f.onComplete {
      case Failure(e) =>
        logger.errorC(s"module type check failed mr=${mrId.value}", e)
      case _ => ()
    }
  }

  // TODO: class MailComposer?
  private def handleReviewReject(module: UUID)(using id: CorrelationId): Unit = {
    def sendMail(rejectedReview: ModuleReview.Atomic) = {
      for
        moduleTitle <- moduleDraftRepository.getModuleTitle(module)
        users       <- moduleUpdatePermissionRepository.allPeopleWithPermissionForModule(module)
      yield {
        logger.infoC(s"module review rejected module=$module")
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
            logger.warnC(
              s"module review rejection notification skipped module=$module reason=missing_inherited_permissions_recipient"
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
      case Failure(e) =>
        logger.errorC(s"module review rejection notification failed module=$module", e)
      case _ => ()
    }
  }

  private def abort(result: ParsedMergeEvent)(using CorrelationId): Unit =
    logger.warnC(
      s"merge skipped mr=${result.mrId.value} action=${result.action} ${result.sourceBranch.value}->${result.targetBranch.value} labels=${result.labels.mkString(",")} reason=unsupported_event_shape"
    )
}
