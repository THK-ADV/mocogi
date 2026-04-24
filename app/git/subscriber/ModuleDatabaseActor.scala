package git.subscriber

import java.time.LocalDateTime
import javax.inject.Inject

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

import database.repo.schedule.ModuleTeachingUnitRepository
import database.view.ModuleViewRepository
import git.subscriber.ModuleSubscribers.Handle
import logging.AppEventLogger
import logging.CorrelationId
import logging.LogEvent
import logging.LogResult
import org.apache.pekko.actor.Actor
import parsing.types.Module
import play.api.Logging
import service.ModuleCreationService
import service.ModuleService
import service.ModuleUpdatePermissionService

final class ModuleDatabaseActor @Inject() (
    moduleService: ModuleService,
    moduleViewRepository: ModuleViewRepository,
    moduleUpdatePermissionService: ModuleUpdatePermissionService,
    moduleCreationService: ModuleCreationService,
    moduleTeachingUnitRepository: ModuleTeachingUnitRepository,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  override def receive = {
    case Handle(modules, correlationId) if modules.nonEmpty =>
      val event   = "module.subscriber.database_sync"
      val entries = modules.map(m => (m._1, m._2.lastModified))
      infoEvent(
        event = event,
        result = LogResult.Started,
        correlationId = correlationId,
        details = Map("moduleCount" -> entries.size.toString)
      )
      update(entries).onComplete {
        case Success(_) =>
          infoEvent(
            event = event,
            result = LogResult.Succeeded,
            correlationId = correlationId,
            details = Map("moduleCount" -> entries.size.toString)
          )
        case Failure(e) =>
          AppEventLogger.error(
            logger,
            LogEvent(
              event = event,
              result = LogResult.Failed,
              correlationId = correlationId,
              errorCode = Some("module_database_sync_failed"),
              details = Map("moduleCount" -> entries.size.toString)
            ),
            e
          )
      }
  }

  private def update(modules: Seq[(Module, LocalDateTime)]) =
    for {
      _ <- moduleService.createOrUpdateMany(modules)
      _ <- moduleViewRepository.refreshView()
      _ <- moduleUpdatePermissionService.overrideInherited(
        modules.map {
          case (module, _) =>
            (
              module.metadata.id,
              module.metadata.responsibilities.moduleManagement
            )
        }
      )
      _ <- moduleCreationService.deleteMany(modules.map(_._1.metadata.id))
      _ <- moduleTeachingUnitRepository.update(modules.map { (m, _) =>
        val pos = mutable.Set[String]()
        m.metadata.pos.mandatory.foreach(po => pos.add(po.po.id))
        m.metadata.pos.optional.foreach(po => pos.add(po.po.id))
        (m.metadata.id, pos.toList)
      })
    } yield ()

  private def infoEvent(
      event: String,
      result: LogResult,
      correlationId: CorrelationId,
      details: Map[String, String] = Map.empty
  ): Unit =
    AppEventLogger.info(
      logger,
      LogEvent(
        event = event,
        result = result,
        correlationId = correlationId,
        details = details
      )
    )
}
