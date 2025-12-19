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
import database.repo.ModuleReviewRepository
import database.repo.ModuleUpdatePermissionRepository
import git.*
import git.api.GitCommitService
import git.api.GitFileService
import git.api.GitMergeRequestService
import io.circe.ParsingFailure
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
import service.pipeline.Print
import service.ModuleCreationService

final class MergeEventHandler @Inject() (
    gitConfig: GitConfig,
    moduleReviewRepository: ModuleReviewRepository,
    moduleDraftRepository: ModuleDraftRepository,
    moduleCreationService: ModuleCreationService,
    mergeRequestApiService: GitMergeRequestService,
    gitCommitService: GitCommitService,
    moduleUpdatePermissionRepository: ModuleUpdatePermissionRepository,
    messages: MessagesApi,
    fileService: GitFileService,
    @Named("MailActor") mailActor: ActorRef,
    @Named("moduleEditUrl") moduleEditUrl: String,
    modulePipeline: MetadataPipeline,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  private type Action      = String
  private type Labels      = IndexedSeq[String]
  private type ParseResult = (MergeRequestId, Action, Branch, Branch, Labels)

  given Lang(Locale.GERMANY)

  private val maxMergeRetries = 10

  override def receive: Receive = {
    case HandleEvent(json) =>
      implicit val id: UUID = UUID.randomUUID()
      parse(json) match {
        case JsSuccess(jsonResult, _) =>
          implicit val result: ParseResult  = jsonResult
          implicit val mrId: MergeRequestId = result._1
          val action                        = result._2
          val sourceBranch                  = result._3
          val targetBranch                  = result._4
          val labels                        = result._5
          (sourceBranch, targetBranch, action) match {
            // Case 1: opened MR from $module_branch into draft branch [AUTO APPROVED]
            // => schedule merge
            case (moduleBranch, gitConfig.draftBranch, "open") if labels.contains(gitConfig.autoApprovedLabel) =>
              logEvent(action, sourceBranch, targetBranch, labels)
              scheduleFreshMerge(moduleBranch)

            // Case 2: merged MR from $module_branch into draft branch [AUTO APPROVED or REVIEW REQUIRED]
            // => delete module draft, update permissions and create module if it's new
            case (moduleBranch, gitConfig.draftBranch, "merge")
                if labels.contains(gitConfig.autoApprovedLabel) || labels.contains(gitConfig.reviewRequiredLabel) =>
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
      logger.info(s"[$id][${Thread.currentThread().getName.last}] merging...")
      val f = for {
        mrStatus <- mergeRequestApiService.merge(mrID)
        _        <- moduleDraftRepository.updateMergeRequestStatus(moduleId, mrStatus)
      } yield logger.info(
        s"[$id][${Thread.currentThread().getName.last}] successfully merged request with id ${mrID.value}"
      )
      f.onComplete {
        case Success(_) =>
          self ! Finished(id)
        case Failure(e) =>
          logger.error(s"[$id][${Thread.currentThread().getName.last}] failed to merge module", e)
          self ! Finished(id)
      }

    // Check if the merge request is mergeable. If so, merge the module (case 2). Otherwise, schedule a new attempt with logarithmic backoff
    case CheckMrStatus(id, mrId, attempt, merge) =>
      if (attempt < maxMergeRetries) {
        logger.info(s"[$id][${Thread.currentThread().getName.last}] attempt $attempt")
        mergeRequestApiService.get(mrId).onComplete {
          case Success((_, json)) =>
            val detailedMergeStatus = json.\("detailed_merge_status").validate[String].get
            val mergeStatus         = json.\("merge_status").validate[String].get
            logger.info(
              s"[$id][${Thread.currentThread().getName.last}] mergeStatus: $mergeStatus, detailedMergeStatus: $detailedMergeStatus"
            )
            if (detailedMergeStatus == "mergeable" && mergeStatus == "can_be_merged") {
              merge()
            } else {
              scheduleMerge(attempt + 1, merge)(id, mrId)
            }
          case Failure(e) =>
            logger.error(s"[$id][${Thread.currentThread().getName.last}] failed to check merge request status", e)
            self ! Finished(id)
        }
      } else {
        logger.info(s"[$id][${Thread.currentThread().getName.last}] no attempts left ($attempt / $maxMergeRetries)")
        self ! Finished(id)
      }

    case Finished(id) =>
      logger.info(s"[$id][${Thread.currentThread().getName.last}] finished!")
  }

  private def logEvent(action: Action, source: Branch, target: Branch, labels: Labels)(implicit id: UUID): Unit =
    logger.info(
      s"[$id][${Thread.currentThread().getName.last}] $action $source -> $target [${labels.mkString(", ")}]"
    )

  private def scheduleFreshMerge(
      moduleBranch: Branch
  )(implicit id: UUID, mrId: MergeRequestId, result: ParseResult): Unit =
    withUUID(moduleBranch)(moduleId => scheduleMerge(0, () => self ! MergeModule(id, mrId, moduleId)))

  private case class MergeModule(id: UUID, mrId: MergeRequestId, moduleId: UUID)

  private case class Finished(id: UUID)

  private case class CheckMrStatus(id: UUID, mrId: MergeRequestId, attempt: Int, merge: () => Unit)

  private def scheduleMerge(attempt: Int, merge: () => Unit)(implicit id: UUID, mrId: MergeRequestId) = {
    val delay = Math.pow(2, attempt).seconds + 3.seconds
    if attempt > 0 then logger.info(s"[$id][${Thread.currentThread().getName.last}] retrying in $delay")
    context.system.scheduler.scheduleOnce(delay, self, CheckMrStatus(id, mrId, attempt, merge))
  }

  private def withUUID(branch: Branch)(k: UUID => Unit)(implicit id: UUID, result: ParseResult): Unit =
    try {
      val moduleId = UUID.fromString(branch.value)
      k(moduleId)
    } catch {
      case NonFatal(_) =>
        logger.error(
          s"[$id][${Thread.currentThread().getName.last}] expected source branch to be a module, but was ${branch.value}"
        )
        abort(id, result)
        self ! Finished(id)
    }

  private def parseMergeCommitSha(json: JsValue): String =
    json.\("object_attributes").\("merge_commit_sha").validate[String].get

  private def parse(json: JsValue): JsResult[ParseResult] = {
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
    } yield (
      mrId,
      action,
      sourceBranch,
      targetBranch,
      labels
    )
  }

  private def deleteModuleDraft(id: UUID, moduleId: UUID) =
    for
      res1 <- moduleReviewRepository.delete(moduleId)
      res2 <- moduleDraftRepository.delete(moduleId)
    yield logger.info(
      s"[$id][${Thread.currentThread().getName.last}] deleted $res1 module reviews and $res2 module drafts"
    )

  private def handleModuleCreated(id: UUID, moduleId: UUID, sha: String): Unit = {
    val f = for {
      (module, diff) <- gitCommitService.getLatestModuleFromCommit(sha, gitConfig.draftBranch, moduleId).collect {
        case Some((content, diff)) => (parseCreatedModuleInformation(content, moduleId), diff)
      }
      _ <- createNewModuleWithPermissions(id, module, diff)
      _ <- deleteModuleDraft(id, moduleId)
    } yield ()
    f.onComplete {
      case Success(_) =>
        self ! Finished(id)
      case Failure(e) =>
        logger.error(s"[$id][${Thread.currentThread().getName.last}]", e)
        self ! Finished(id)
    }
  }

  private def createNewModuleWithPermissions(id: UUID, module: CreatedModule, diff: CommitDiff) =
    moduleCreationService.createOrUpdateWithPermissions(module).map { _ =>
      val prefixStr = if diff.isNewFile then "created new module" else "updated module"
      logger.info(
        s"[$id][${Thread.currentThread().getName.last}] $prefixStr ${module.module} with ${module.moduleManagement.size} permissions"
      )
    }

  private def parseCreatedModuleInformation(content: GitFileContent, module: => UUID) =
    try RawModuleParser.parseCreatedModuleInformation(content.value)
    catch
      case pf: ParsingFailure => throw YamlParsingError(module, pf)
      case pe: ParsingError   => throw YamlParsingError(module, pe)
      case NonFatal(e)        => throw YamlParsingError(module, e)

  private def handleModuleBulkUpdate(id: UUID, sha: String): Unit = {
    val f = for
      downloads <- gitCommitService.getAllModulesFromCommit(sha, gitConfig.draftBranch)
      _         <- Future.sequence(downloads.map { (content, diff) =>
        val module = parseCreatedModuleInformation(content, diff.newPath.moduleId(gitConfig).get)
        createNewModuleWithPermissions(id, module, diff)
      })
    yield ()

    f.onComplete {
      case Success(_) =>
        self ! Finished(id)
      case Failure(e) =>
        logger.error(s"[$id][${Thread.currentThread().getName.last}]", e)
        self ! Finished(id)
    }
  }

  private def typeCheckModules(branch: Branch)(implicit id: UUID, mrId: MergeRequestId): Unit = {
    logger.info(s"[$id][${Thread.currentThread().getName.last}] type checking modules of MR ${mrId.value}…")
    val f = for
      changes   <- mergeRequestApiService.getChanges(mrId)
      downloads <- Future.sequence(changes.collect {
        case d if d.path.isModule(gitConfig) => fileService.download(d.path, branch)
      })
      _ <-
        if downloads.isEmpty then
          Future.successful(logger.info(s"[$id][${Thread.currentThread().getName.last}] no module files to check"))
        else {
          for {
            parseRes <- modulePipeline.parseValidateMany(downloads.collect { case Some(f) => Print(f._1.value) })
            _        <- parseRes match {
              case Left(errs) =>
                logger.error(
                  s"[$id][${Thread.currentThread().getName.last}] type checking revealed ${errs.size} errors"
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
                  .map(_ => logger.info(s"[$id][${Thread.currentThread().getName.last}] all modules are sound"))
            }
          } yield ()
        }
    yield ()

    f.onComplete {
      case Success(_) =>
        self ! Finished(id)
      case Failure(e) =>
        logger.error(s"[$id][${Thread.currentThread().getName.last}]", e)
        self ! Finished(id)
    }
  }

  // TODO: class MailComposer?
  private def handleReviewReject(module: UUID)(using id: UUID): Unit = {
    def sendMail(rejectedReview: ModuleReview.Atomic) = {
      for
        moduleTitle <- moduleDraftRepository.getModuleTitle(module)
        users       <- moduleUpdatePermissionRepository.allPeopleWithPermissionForModule(module)
      yield {
        logger.info(s"[$id][${Thread.currentThread().getName.last}] module review for $module got rejected")
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
            logger.error(
              s"[$id][${Thread.currentThread().getName.last}] expected at least one user with inherited permission, but found none"
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
        logger.error(s"[$id][${Thread.currentThread().getName.last}]", e)
        self ! Finished(id)
    }
  }

  private def abort(id: UUID, result: ParseResult): Unit =
    logger.info(
      s"""[$id][${Thread.currentThread().getName.last}] unable to handle event
         |- merge request id: ${result._1.value}
         |- action: ${result._2}
         |- source: ${result._3.value}
         |- target ${result._4.value}
         |- labels: ${result._5}""".stripMargin
    )
}
