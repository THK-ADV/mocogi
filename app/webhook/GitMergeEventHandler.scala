package webhook

import akka.actor.{Actor, ActorRef, Props}
import catalog.ModuleCatalogService
import database.repo.{
  ModuleCatalogGenerationRequestRepository,
  ModuleDraftRepository,
  ModuleReviewRepository
}
import git.GitConfig
import git.api.{GitBranchService, GitMergeRequestApiService}
import models._
import ops.LoggerOps
import play.api.Logging
import play.api.libs.json._

import java.nio.file.Paths
import java.util.UUID
import javax.inject.Singleton
import scala.collection.IndexedSeq
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process.{Process, ProcessLogger}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

@Singleton
case class GitMergeEventHandler(private val value: ActorRef) {
  def handle(json: JsValue): Unit =
    value ! HandleEvent(json)
}

object GitMergeEventHandler {
  private type Action = String
  private type Labels = IndexedSeq[String]
  private type ParseResult = (MergeRequestId, Action, Branch, Branch, Labels)

  def props(
      gitConfig: GitConfig,
      moduleReviewRepository: ModuleReviewRepository,
      moduleDraftRepository: ModuleDraftRepository,
      moduleCatalogGenerationRepo: ModuleCatalogGenerationRequestRepository,
      mergeRequestApiService: GitMergeRequestApiService,
      branchService: GitBranchService,
      moduleCatalogService: ModuleCatalogService,
      bigBangLabel: String,
      moduleCatalogLabel: String,
      autoApprovedLabel: String,
      reviewRequiredLabel: String,
      moduleCatalogGenerationDelay: FiniteDuration,
      maxMergeRetries: Int,
      ctx: ExecutionContext
  ) = Props(
    new Impl(
      gitConfig,
      moduleReviewRepository,
      moduleDraftRepository,
      moduleCatalogGenerationRepo,
      mergeRequestApiService,
      branchService,
      moduleCatalogService,
      bigBangLabel,
      moduleCatalogLabel,
      autoApprovedLabel,
      reviewRequiredLabel,
      moduleCatalogGenerationDelay,
      maxMergeRetries,
      ctx
    )
  )

  private class Impl(
      gitConfig: GitConfig,
      moduleReviewRepository: ModuleReviewRepository,
      moduleDraftRepository: ModuleDraftRepository,
      moduleCatalogGenerationRepo: ModuleCatalogGenerationRequestRepository,
      mergeRequestApiService: GitMergeRequestApiService,
      branchService: GitBranchService,
      moduleCatalogService: ModuleCatalogService,
      bigBangLabel: String,
      moduleCatalogLabel: String,
      autoApprovedLabel: String,
      reviewRequiredLabel: String,
      moduleCatalogGenerationDelay: FiniteDuration,
      maxMergeRetries: Int,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging
      with LoggerOps {

    override def receive: Receive = {
      case HandleEvent(json) =>
        implicit val id: UUID = UUID.randomUUID()
        parse(json) match {
          case JsSuccess(jsonResult, _) =>
            implicit val result: ParseResult = jsonResult
            implicit val mrId: MergeRequestId = result._1
            val action = result._2
            val sourceBranch = result._3
            val targetBranch = result._4
            val labels = result._5
            (sourceBranch, targetBranch, action) match {
              case (gitConfig.draftBranch, gitConfig.mainBranch, "open")
                  if labels.contains(bigBangLabel) =>
                scheduleBigBangMerge
              case (gitConfig.draftBranch, gitConfig.mainBranch, "merge")
                  if labels.contains(bigBangLabel) =>
                scheduleModuleCatalogCreation
              case (semesterBranch, gitConfig.mainBranch, "open")
                  if labels.contains(moduleCatalogLabel) =>
                scheduleModuleCatalogMerge(Semester(semesterBranch.value))
              case (semesterBranch, gitConfig.mainBranch, "merge")
                  if labels.contains(moduleCatalogLabel) =>
                resetBigBang(Semester(semesterBranch.value))
              case (moduleBranch, gitConfig.draftBranch, "open")
                  if labels.contains(autoApprovedLabel) =>
                withUUID(moduleBranch) { moduleId =>
                  scheduleMerge(
                    0,
                    () => self ! MergeModule(id, mrId, moduleId)
                  )
                }
              case (moduleBranch, gitConfig.draftBranch, "merge")
                  if labels.contains(autoApprovedLabel) ||
                    labels.contains(reviewRequiredLabel) =>
                withUUID(moduleBranch)(id => removeModuleDrafts(id, id))
              case _ =>
                abort(id, result)
            }
          case JsError(errors) =>
            logUnhandedEvent(logger, errors)
            self ! Finished(id)
        }
      case MergeModule(id, mrID, moduleId) =>
        mergeModuleBranch(id, mrID, moduleId) onComplete {
          case Success(_) =>
            self ! Finished(id)
          case Failure(e) =>
            logFailure(e)
            self ! Finished(id)
        }
      case MergePreview(id, mrId, r) =>
        mergePreviewBranch(id, mrId, r) onComplete {
          case Success(_) =>
            self ! Finished(id)
          case Failure(e) =>
            logFailure(e)
            self ! Finished(id)
        }
      case MergeModuleCatalog(id, mrId, r) =>
        mergeModuleCatalogBranch(id, mrId, r) onComplete {
          case Success(_) =>
            self ! Finished(id)
          case Failure(e) =>
            logFailure(e)
            self ! Finished(id)
        }
      case CreateModuleCatalogs(id, r) =>
        logger.info(
          s"[$id][${Thread.currentThread().getName.last}] starting with module catalog generation..."
        )
        moduleCatalogService.createAndOpenMergeRequest(r).onComplete {
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
              if (
                detailedMergeStatus == "mergeable" && mergeStatus == "can_be_merged"
              ) {
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
        mrId: MergeRequestId,
        request: ModuleCatalogGenerationRequest
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
        request: ModuleCatalogGenerationRequest
    )

    private case class CheckMrStatus(
        id: UUID,
        mrId: MergeRequestId,
        attempt: Int,
        max: Int,
        merge: () => Unit
    )

    private def scheduleModuleCatalogMerge(semester: Semester)(implicit
        id: UUID,
        mrId: MergeRequestId
    ): Unit = moduleCatalogGenerationRepo.get(mrId, semester.id).onComplete {
      case Success(r) =>
        scheduleMerge(0, () => self ! MergeModuleCatalog(id, mrId, r))
      case Failure(e) =>
        logFailure(e)
        self ! Finished(id)
    }

    private def scheduleModuleCatalogCreation(implicit
        id: UUID,
        mrId: MergeRequestId
    ): Unit =
      moduleCatalogGenerationRepo.get(mrId).onComplete {
        case Success(r) =>
          logger.info(
            s"[$id][${Thread.currentThread().getName.last}] scheduling module catalog generating in $moduleCatalogGenerationDelay"
          )
          context.system.scheduler.scheduleOnce(
            moduleCatalogGenerationDelay,
            self,
            CreateModuleCatalogs(id, r)
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

    private def scheduleBigBangMerge(implicit
        id: UUID,
        mrId: MergeRequestId
    ): Unit =
      moduleCatalogGenerationRepo.get(mrId).onComplete {
        case Success(r) =>
          scheduleMerge(0, () => self ! MergePreview(id, mrId, r))
        case Failure(e) =>
          logFailure(e)
          self ! Finished(id)
      }

    private def resetBigBang(semester: Semester)(implicit
        id: UUID,
        mrId: MergeRequestId
    ): Unit = {
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

      f onComplete {
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
        _ <- moduleCatalogGenerationRepo.update(mrStatus, request)
      } yield logger.info(
        s"[$id][${Thread.currentThread().getName.last}] successfully merged request with id ${mrId.value}"
      )
    }

    private def mergeModuleCatalogBranch(
        id: UUID,
        mrId: MergeRequestId,
        request: ModuleCatalogGenerationRequest
    ) = {
      logger.info(s"[$id][${Thread.currentThread().getName.last}] merging...")
      for {
        mrStatus <- mergeRequestApiService.merge(mrId)
        _ <- moduleCatalogGenerationRepo.update(mrStatus, request)
      } yield logger.info(
        s"[$id][${Thread.currentThread().getName.last}] successfully merged request with id ${mrId.value}"
      )
    }

    private def parse(json: JsValue): JsResult[ParseResult] = {
      val attrs = json.\("object_attributes")
      for {
        mrId <- attrs.\("iid").validate[Int].map(MergeRequestId.apply)
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

    private def removeModuleDrafts(id: UUID, moduleId: UUID): Unit = {
      logger.info(
        s"[$id][${Thread.currentThread().getName.last}] deleting module draft with id $moduleId"
      )
      val f = for {
        res1 <- moduleReviewRepository.delete(moduleId)
        res2 <- moduleDraftRepository.delete(moduleId)
      } yield logger.info(
        s"[$id][${Thread.currentThread().getName.last}] successfully deleted $res1 module reviews and $res2 module drafts"
      )
      f onComplete {
        case Success(_) =>
          self ! Finished(id)
        case Failure(e) =>
          logFailure(e)
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
