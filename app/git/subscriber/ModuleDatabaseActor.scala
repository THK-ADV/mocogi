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
    case Handle(modules) if modules.nonEmpty =>
      val entries = modules.map(m => (m._1, m._2.lastModified))
      update(entries).onComplete {
        case Success(_) =>
          logger.info(s"successfully created or updated ${entries.size} modules")
        case Failure(e) =>
          logger.error(
            s"""failed to create or update metadata
               |  - message: ${e.getMessage}
               |  - trace: ${e.getStackTrace.mkString(
                "\n           "
              )}""".stripMargin
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
      _ <- moduleTeachingUnitRepository.recreate(modules.map { (m, _) =>
        val pos = mutable.Set[String]()
        m.metadata.pos.mandatory.foreach(po => pos.add(po.po.id))
        m.metadata.pos.optional.foreach(po => pos.add(po.po.id))
        (m.metadata.id, pos.toList)
      })
    } yield ()
}
