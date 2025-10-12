package webhook

import java.nio.file.Paths
import java.util.Locale
import java.util.UUID
import javax.inject.Singleton

import scala.collection.IndexedSeq
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.sys.process.Process
import scala.sys.process.ProcessLogger
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Success

import catalog.ModuleCatalogService
import cats.data.NonEmptyList
import database.repo.ModuleCatalogGenerationRequestRepository
import database.repo.ModuleDraftRepository
import database.repo.ModuleReviewRepository
import database.repo.ModuleUpdatePermissionRepository
import git.*
import git.api.GitBranchService
import git.api.GitCommitService
import git.api.GitMergeRequestApiService
import io.circe.ParsingFailure
import models.*
import ops.LoggerOps
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.Props
import parser.ParsingError
import parsing.RawModuleParser
import parsing.YamlParsingError
import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import play.api.libs.json.*
import play.api.Logging
import service.mail.MailerService
import service.ModuleCreationService

@Singleton
case class GitMergeEventHandler(private val value: ActorRef) {
  def handle(json: JsValue): Unit =
    value ! HandleEvent(json)
}

object GitMergeEventHandler {
  private type Action      = String
  private type Labels      = IndexedSeq[String]
  private type ParseResult = (MergeRequestId, Action, Branch, Branch, Labels)

  def props(
      gitConfig: GitConfig,
      moduleReviewRepository: ModuleReviewRepository,
      moduleDraftRepository: ModuleDraftRepository,
      moduleCreationService: ModuleCreationService,
      moduleCatalogGenerationRepo: ModuleCatalogGenerationRequestRepository,
      mergeRequestApiService: GitMergeRequestApiService,
      branchService: GitBranchService,
      gitCommitService: GitCommitService,
      moduleCatalogService: ModuleCatalogService,
      moduleUpdatePermissionRepository: ModuleUpdatePermissionRepository,
      mailerService: MailerService,
      messages: MessagesApi,
      bigBangLabel: String,
      moduleCatalogLabel: String,
      autoApprovedLabel: String,
      reviewRequiredLabel: String,
      moduleCatalogGenerationDelay: FiniteDuration,
      maxMergeRetries: Int,
      moduleEditUrl: String,
      ctx: ExecutionContext
  ) = Props(
    new Impl(
      gitConfig,
      moduleReviewRepository,
      moduleDraftRepository,
      moduleCreationService,
      moduleCatalogGenerationRepo,
      mergeRequestApiService,
      branchService,
      gitCommitService,
      moduleCatalogService,
      moduleUpdatePermissionRepository,
      mailerService,
      messages,
      bigBangLabel,
      moduleCatalogLabel,
      autoApprovedLabel,
      reviewRequiredLabel,
      moduleCatalogGenerationDelay,
      maxMergeRetries,
      moduleEditUrl,
      ctx
    )
  )

  private class Impl(
      gitConfig: GitConfig,
      moduleReviewRepository: ModuleReviewRepository,
      moduleDraftRepository: ModuleDraftRepository,
      moduleCreationService: ModuleCreationService,
      moduleCatalogGenerationRepo: ModuleCatalogGenerationRequestRepository,
      mergeRequestApiService: GitMergeRequestApiService,
      branchService: GitBranchService,
      gitCommitService: GitCommitService,
      moduleCatalogService: ModuleCatalogService,
      moduleUpdatePermissionRepository: ModuleUpdatePermissionRepository,
      mailerService: MailerService,
      messages: MessagesApi,
      bigBangLabel: String,
      moduleCatalogLabel: String,
      autoApprovedLabel: String,
      reviewRequiredLabel: String,
      moduleCatalogGenerationDelay: FiniteDuration,
      maxMergeRetries: Int,
      moduleEditUrl: String,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging
      with LoggerOps {

    given Lang(Locale.GERMANY)

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
              case (gitConfig.draftBranch, gitConfig.mainBranch, "open") if labels.contains(bigBangLabel) =>
                scheduleBigBangMerge
              case (gitConfig.draftBranch, gitConfig.mainBranch, "merge") if labels.contains(bigBangLabel) =>
                scheduleModuleCatalogCreation
              case (_, gitConfig.mainBranch, "open") if labels.contains(moduleCatalogLabel) =>
                scheduleModuleCatalogMerge
              case (semesterBranch, gitConfig.mainBranch, "merge") if labels.contains(moduleCatalogLabel) =>
                resetBigBang(Semester(semesterBranch.value))
              case (moduleBranch, gitConfig.draftBranch, "open") if labels.contains(autoApprovedLabel) =>
                withUUID(moduleBranch) { moduleId =>
                  scheduleMerge(
                    0,
                    () => self ! MergeModule(id, mrId, moduleId)
                  )
                }
              case (moduleBranch, gitConfig.draftBranch, "merge")
                  if labels.contains(autoApprovedLabel) ||
                    labels.contains(reviewRequiredLabel) =>
                val sha = json.\("object_attributes").\("merge_commit_sha").validate[String].get
                withUUID(moduleBranch)(moduleId => handleModuleCreated(id, moduleId, sha))
              case (_, gitConfig.draftBranch, "merge") =>
                val sha = json.\("object_attributes").\("merge_commit_sha").validate[Action].get
                handleModuleBulkUpdate(id, sha)
              case (moduleBranch, gitConfig.draftBranch, "close") if labels.contains(reviewRequiredLabel) =>
                withUUID(moduleBranch)(moduleId => handleReviewReject(moduleId))
              case _ =>
                abort(id, result)
            }
          case JsError(errors) =>
            logUnhandedEvent(logger, errors)
            self ! Finished(id)
        }
      case MergeModule(id, mrID, moduleId) =>
        mergeModuleBranch(id, mrID, moduleId).onComplete {
          case Success(_) =>
            self ! Finished(id)
          case Failure(e) =>
            logFailure(e)
            self ! Finished(id)
        }
      case MergePreview(id, mrId, r) =>
        mergePreviewBranch(id, mrId, r).onComplete {
          case Success(_) =>
            self ! Finished(id)
          case Failure(e) =>
            logFailure(e)
            self ! Finished(id)
        }
      case MergeModuleCatalog(id, mrId) =>
        mergeModuleCatalogBranch(id, mrId).onComplete {
          case Success(_) =>
            self ! Finished(id)
          case Failure(e) =>
            logFailure(e)
            self ! Finished(id)
        }
      case CreateModuleCatalogs(id, semesterId) =>
        logger.info(
          s"[$id][${Thread.currentThread().getName.last}] starting with module catalog generation..."
        )
        moduleCatalogService
          .createAndOpenMergeRequest(semesterId)
          .onComplete {
            case Success(_) =>
              self ! Finished(id)
            case Failure(e) =>
              logFailure(e)
              self ! Finished(id)
          }
      case CheckMrStatus(id, mrId, attempt, max, merge) =>
        if (attempt < max) {
          logger.info(
            s"[$id][${Thread.currentThread().getName.last}] attempt $attempt"
          )
          mergeRequestApiService.get(mrId).onComplete {
            case Success((_, json)) =>
              val detailedMergeStatus =
                json.\("detailed_merge_status").validate[String].get
              val mergeStatus = json.\("merge_status").validate[String].get
              logger.info(
                s"[$id][${Thread.currentThread().getName.last}] mergeStatus: $mergeStatus, detailedMergeStatus: $detailedMergeStatus"
              )
              if (detailedMergeStatus == "mergeable" && mergeStatus == "can_be_merged") {
                merge()
              } else {
                scheduleMerge(attempt + 1, merge)(id, mrId)
              }
            case Failure(e) =>
              logFailure(e)
              self ! Finished(id)
          }
        } else {
          logger.info(
            s"[$id][${Thread.currentThread().getName.last}] no attempts left ($attempt / $max)"
          )
          self ! Finished(id)
        }
      case MakeChange(id) =>
        val builder = new StringBuilder()
        val pLogger =
          ProcessLogger(
            a => builder.append(s"$a\n"),
            a => builder.append(s"${Console.RED}$a${Console.RESET}\n")
          )
        val process = Process(
          command = Seq(
            "/bin/bash",
            "../../bigbang_test.sh"
          ),
          cwd = Paths.get("gitlab/modulhandbuecher_test").toAbsolutePath.toFile
        )
        val res = process ! pLogger
        if (res == 0) {
          logger.info(
            s"[$id][${Thread.currentThread().getName.last}] successfully executed bigbang script"
          )
          self ! Finished(id)
        } else {
          logger.error(
            s"[$id][${Thread.currentThread().getName.last}] err executing bigbang script: ${builder.toString()}"
          )
          self ! Finished(id)
        }
      case Finished(id) =>
        logger.info(s"[$id][${Thread.currentThread().getName.last}] finished!")
    }

    private case class MergeModule(
        id: UUID,
        mrId: MergeRequestId,
        moduleId: UUID
    )

    private case class MergeModuleCatalog(
        id: UUID,
        mrId: MergeRequestId
    )

    private case class MergePreview(
        id: UUID,
        mrId: MergeRequestId,
        request: ModuleCatalogGenerationRequest
    )

    private case class Finished(id: UUID)

    private case class MakeChange(id: UUID)

    private case class CreateModuleCatalogs(
        id: UUID,
        semesterId: String
    )

    private case class CheckMrStatus(
        id: UUID,
        mrId: MergeRequestId,
        attempt: Int,
        max: Int,
        merge: () => Unit
    )

    private def scheduleModuleCatalogMerge(implicit id: UUID, mrId: MergeRequestId): Unit = {
      scheduleMerge(
        0,
        () => self ! MergeModuleCatalog(id, mrId)
      )
      self ! Finished(id)
    }

    private def scheduleModuleCatalogCreation(implicit id: UUID, mrId: MergeRequestId): Unit =
      moduleCatalogGenerationRepo.get(mrId).onComplete {
        case Success(r) =>
          logger.info(
            s"[$id][${Thread.currentThread().getName.last}] scheduling module catalog generating in $moduleCatalogGenerationDelay"
          )
          context.system.scheduler.scheduleOnce(
            moduleCatalogGenerationDelay,
            self,
            CreateModuleCatalogs(id, r.semesterId)
          )
          self ! Finished(id)
        case Failure(e) =>
          logFailure(e)
          self ! Finished(id)
      }

    private def scheduleMerge(
        attempt: Int,
        merge: () => Unit
    )(implicit id: UUID, mrId: MergeRequestId) =
      context.system.scheduler.scheduleOnce(
        3.seconds,
        self,
        CheckMrStatus(id, mrId, attempt, maxMergeRetries, merge)
      )

    private def scheduleBigBangMerge(implicit id: UUID, mrId: MergeRequestId): Unit =
      moduleCatalogGenerationRepo.get(mrId).onComplete {
        case Success(r) =>
          scheduleMerge(0, () => self ! MergePreview(id, mrId, r))
        case Failure(e) =>
          logFailure(e)
          self ! Finished(id)
      }

    private def resetBigBang(semester: Semester)(implicit id: UUID, mrId: MergeRequestId): Unit = {
      val f = for {
        _ <- moduleCatalogGenerationRepo.delete(mrId, semester.id)
        _ <- branchService.createPreviewBranch()
      } yield {
        logger.info(
          s"[$id][${Thread.currentThread().getName.last}] successfully deleted module generation request with mr id $mrId and semester ${semester.id}"
        )
        logger.info(
          s"[$id][${Thread.currentThread().getName.last}] successfully created a new ${gitConfig.draftBranch.value} branch"
        )
      }

      f.onComplete {
        case Success(_) =>
          self ! Finished(id)
        case Failure(e) =>
          logFailure(e)
          self ! Finished(id)
      }
    }

    private def withUUID(
        branch: Branch
    )(k: UUID => Unit)(implicit id: UUID, result: ParseResult): Unit =
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

    private def mergeModuleBranch(
        id: UUID,
        mrId: MergeRequestId,
        moduleId: UUID
    ): Future[Unit] = {
      logger.info(s"[$id][${Thread.currentThread().getName.last}] merging...")
      for {
        mrStatus <- mergeRequestApiService.merge(mrId)
        _ <- moduleDraftRepository.updateMergeRequestStatus(
          moduleId,
          mrStatus
        )
      } yield logger.info(
        s"[$id][${Thread.currentThread().getName.last}] successfully merged request with id ${mrId.value}"
      )
    }

    private def mergePreviewBranch(
        id: UUID,
        mrId: MergeRequestId,
        request: ModuleCatalogGenerationRequest
    ) = {
      logger.info(s"[$id][${Thread.currentThread().getName.last}] merging...")
      for {
        mrStatus <- mergeRequestApiService.merge(mrId)
        _        <- moduleCatalogGenerationRepo.update(mrStatus, request)
      } yield logger.info(
        s"[$id][${Thread.currentThread().getName.last}] successfully merged request with id ${mrId.value}"
      )
    }

    private def mergeModuleCatalogBranch(
        id: UUID,
        mrId: MergeRequestId
    ) = {
      logger.info(s"[$id][${Thread.currentThread().getName.last}] merging...")
      mergeRequestApiService
        .merge(mrId)
        .map(_ =>
          logger.info(
            s"[$id][${Thread.currentThread().getName.last}] successfully merged request with id ${mrId.value}"
          )
        )
    }

    private def parse(json: JsValue): JsResult[ParseResult] = {
      val attrs = json.\("object_attributes")
      for {
        mrId   <- attrs.\("iid").validate[Int].map(MergeRequestId.apply)
        action <- attrs.\("action").validate[String]
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
        _ <- Future.sequence(downloads.map { (content, diff) =>
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
              mailerService.sendMail(
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
        _ <- {
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
}
