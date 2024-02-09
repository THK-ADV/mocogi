package webhook

import akka.actor.{Actor, ActorRef, Props}
import database.repo.{
  ModuleCatalogGenerationRequestRepository,
  ModuleDraftRepository,
  ModuleReviewRepository
}
import git.GitConfig
import git.api.{GitBranchService, GitMergeRequestApiService}
import models.{Branch, MergeRequestId}
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
      mergeRequestApiService: GitMergeRequestApiService,
      branchService: GitBranchService,
      bigBangLabel: String,
      moduleCatalogLabel: String,
      autoApprovedLabel: String,
      reviewRequiredLabel: String,
      moduleCatalogGenerationDelay: FiniteDuration,
      moduleCatalogActor: ActorRef,
      moduleCatalogGenerationRepo: ModuleCatalogGenerationRequestRepository,
      maxMergeRetries: Int,
      ctx: ExecutionContext
  ) = Props(
    new Impl(
      gitConfig,
      moduleReviewRepository,
      moduleDraftRepository,
      mergeRequestApiService,
      branchService,
      bigBangLabel,
      moduleCatalogLabel,
      autoApprovedLabel,
      reviewRequiredLabel,
      moduleCatalogGenerationDelay,
      moduleCatalogActor,
      moduleCatalogGenerationRepo,
      maxMergeRetries,
      ctx
    )
  )

  private class Impl(
      gitConfig: GitConfig,
      moduleReviewRepository: ModuleReviewRepository,
      moduleDraftRepository: ModuleDraftRepository,
      mergeRequestApiService: GitMergeRequestApiService,
      branchService: GitBranchService,
      bigBangLabel: String,
      moduleCatalogLabel: String,
      autoApprovedLabel: String,
      reviewRequiredLabel: String,
      moduleCatalogGenerationDelay: FiniteDuration,
      moduleCatalogActor: ActorRef,
      moduleCatalogGenerationRepo: ModuleCatalogGenerationRequestRepository,
      maxMergeRetries: Int,
      implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging
      with LoggerOps {

//    private def handleEvent(json: JsValue): Unit = {
//      parse(json) match {
//        case JsSuccess(result, _) =>
//          val targetBranch = result._6
//          val sourceBranch = result._5
//          val mrId = result._1
//          val mergeStatus = result._2
//          val state = result._3
//          val action = result._4
//          val labels = result._7
//
//          val res = (sourceBranch, targetBranch, mergeStatus, state) match {
//            case (
//                  gitConfig.draftBranch,
//                  gitConfig.mainBranch,
//                  "mergeable",
//                  "opened"
//                ) if labels.contains(bigBangLabel) =>
//              logger.info(
//                s"[$id][${Thread.currentThread().getName.last}] merging ${sourceBranch.value} into ${targetBranch.value}"
//              )
//              for {
//                request <- moduleCatalogGenerationRepo.get(mrId)
//                _ <-
//                  if (request.status != MergeRequestStatus.Open)
//                    abort(id, result)
//                  else
//                    for {
//                      status <- mergeRequestApiService.merge(mrId)
//                      _ = logger.info(
//                        s"[$id][${Thread.currentThread().getName.last}] successfully merged request with id ${mrId.value}"
//                      )
//                      _ <- moduleCatalogGenerationRepo.update(
//                        status,
//                        request
//                      )
//                      _ = logger.info(
//                        s"[$id][${Thread.currentThread().getName.last}] successfully updated generation request with id ${mrId.value} to status ${status.id}"
//                      )
//                    } yield context.system.scheduler.scheduleOnce(
//                      moduleCatalogGenerationDelay,
//                      moduleCatalogActor,
//                      GenerateLatexFiles(request)
//                    )
//              } yield logger.info(
//                s"[$id][${Thread.currentThread().getName.last}] successfully scheduled module catalog generation for ${request.semesterId} in $moduleCatalogGenerationDelay"
//              )
//            case (
//                  semesterBranch,
//                  gitConfig.mainBranch,
//                  "mergeable",
//                  "opened"
//                ) =>
//              logger.info(
//                s"[$id][${Thread.currentThread().getName.last}] merging ${sourceBranch.value} into ${targetBranch.value}"
//              )
//              val semester = Semester(semesterBranch.value)
//              for {
//                request <- moduleCatalogGenerationRepo.get(mrId)
//                _ <-
//                  if (
//                    request.status != MergeRequestStatus.Open && request.semesterId == semester.id
//                  ) abort(id, result)
//                  else
//                    for {
//                      _ <- mergeRequestApiService.merge(mrId)
//                      _ = logger.info(
//                        s"[$id][${Thread.currentThread().getName.last}] successfully merged request with id ${mrId.value}"
//                      )
//                      _ <- moduleCatalogGenerationRepo.delete(mrId)
//                      _ = logger.info(
//                        s"[$id][${Thread.currentThread().getName.last}] successfully removed generation request with id ${mrId.value}"
//                      )
//                      _ <- branchService.createPreviewBranch()
//                    } yield logger.info(
//                      s"[$id][${Thread.currentThread().getName.last}] successfully created a new preview branch"
//                    )
//              } yield ()
//            case (moduleBranch, gitConfig.draftBranch, _, _) =>
//              for {
//                moduleId <- Future.fromTry(
//                  Try(UUID.fromString(moduleBranch.value))
//                )
//                res <- (mergeStatus, state, action) match {
//                  case (
//                        "mergeable",
//                        "opened",
//                        _
//                      ) => // TODO auto accepted vs. review
//                    mergeRequestApiService.get(mrId).map {
//                      case (status, json) =>
//                        logger.info(s"[$id][${Thread.currentThread().getName.last}] $status")
//                        logger.info(s"[$id][${Thread.currentThread().getName.last}] $json")
//                    }
////                    if (!ongoingMerge.get()) {
////                      ongoingMerge.set(true)
////                      logger.info(
////                        s"[$id][${Thread.currentThread().getName.last}] merging ${sourceBranch.value} into ${targetBranch.value}"
////                      )
////                      merge(id, mrId, moduleId)
////                    } else {
////                      logger.info(s"[$id][${Thread.currentThread().getName.last}] handling the merge is ongoing")
////                      Future.unit
////                    }
//                  case ("not_open", "merged", "merge") =>
//                    logger.info(
//                      s"[$id][${Thread.currentThread().getName.last}] ${sourceBranch.value} was merged into ${targetBranch.value}"
//                    )
//                    removeDraft(moduleId)
//                  case _ =>
//                    abort(id, result)
//                }
//              } yield res
//            case _ =>
//              abort(id, result)
//          }
//
//          res onComplete {
//            case Success(_) => logger.info(s"[$id][${Thread.currentThread().getName.last}] finished!")
//            case Failure(e) => logFailure(e)
//          }
//
//        case JsError(errors) =>
//          logUnhandedEvent(logger, errors)
//      }
//    }

    override def receive: Receive = {
      case HandleEvent(json) =>
        val id = UUID.randomUUID()
        parse(json) match {
          case JsSuccess(result, _) =>
            val mrId = result._1
            val action = result._2
            val sourceBranch = result._3
            val targetBranch = result._4
            val labels = result._5
            (sourceBranch, targetBranch, action) match {
              case (gitConfig.draftBranch, gitConfig.mainBranch, "open")
                  if labels.contains(bigBangLabel) =>

              case (semesterBranch, gitConfig.mainBranch, "open")
                  if labels.contains(moduleCatalogLabel) =>
              // TODO semester branch validation
              case (moduleBranch, gitConfig.draftBranch, "open")
                  if labels.contains(autoApprovedLabel) =>
                withUUID(id, result, moduleBranch) { moduleId =>
                  context.system.scheduler.scheduleOnce(
                    3.seconds,
                    self,
                    CheckMrStatus(id, mrId, moduleId, 0, maxMergeRetries)
                  )
                }
              case (moduleBranch, gitConfig.draftBranch, "merge")
                  if labels.contains(autoApprovedLabel) =>
                withUUID(id, result, moduleBranch)(moduleId =>
                  removeModuleDrafts(id, moduleId)
                )
              case _ =>
                logger.info(
                  s"[$id][${Thread.currentThread().getName.last}] unhandled"
                )
            }
          case JsError(errors) =>
            logUnhandedEvent(logger, errors)
        }
      case Merge(id, mrID, moduleId) =>
        merge(id, mrID, moduleId) onComplete {
          case Success(_) =>
            self ! Finished(id)
          case Failure(e) =>
            logFailure(e)
            self ! Finished(id)
        }
      case CheckMrStatus(id, mrId, moduleId, attempt, max) =>
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
              ) self ! Merge(id, mrId, moduleId)
              else
                context.system.scheduler.scheduleOnce(
                  3.seconds,
                  self,
                  CheckMrStatus(id, mrId, moduleId, attempt + 1, max)
                )
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

    private case class Merge(id: UUID, mrId: MergeRequestId, moduleId: UUID)

    private case class Finished(id: UUID)

    private case class MakeChange(id: UUID)

    private case class CheckMrStatus(
        id: UUID,
        mrId: MergeRequestId,
        moduleId: UUID,
        attempt: Int,
        max: Int
    )

    private def handleBigBang(
        id: UUID,
        result: ParseResult,
        mrId: MergeRequestId
    ) = {
//      for {
//        request <- moduleCatalogGenerationRepo.get(mrId)
//        _ <-
//          if (request.status != MergeRequestStatus.Open)
//            abort(id, result)
//          else
//            for {
//              status <- mergeRequestApiService.merge(mrId)
//              _ = logger.info(
//                s"[$id][${Thread.currentThread().getName.last}] successfully merged request with id ${mrId.value}"
//              )
//              _ <- moduleCatalogGenerationRepo.update(
//                status,
//                request
//              )
//              _ = logger.info(
//                s"[$id][${Thread.currentThread().getName.last}] successfully updated generation request with id ${mrId.value} to status ${status.id}"
//              )
//            } yield context.system.scheduler.scheduleOnce(
//              moduleCatalogGenerationDelay,
//              moduleCatalogActor,
//              GenerateLatexFiles(request)
//            )
//      } yield logger.info(
//        s"[$id][${Thread.currentThread().getName.last}] successfully scheduled module catalog generation for ${request.semesterId} in $moduleCatalogGenerationDelay"
//      )
    }

    private def withUUID(id: UUID, result: ParseResult, branch: Branch)(
        k: UUID => Unit
    ): Unit =
      try {
        val moduleId = UUID.fromString(branch.value)
        k(moduleId)
      } catch {
        case NonFatal(_) =>
          logger.error(
            s"[$id][${Thread.currentThread().getName.last}] expected source branch to be a module, but was ${branch.value}"
          )
          abort(id, result)
      }

    private def merge(
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
