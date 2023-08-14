package git.subscriber

import akka.actor.{Actor, Props}
import database.view.ModuleViewRepository
import git.subscriber.ModuleCompendiumSubscribers.{CreatedOrUpdated, Removed}
import play.api.Logging

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ModuleCompendiumViewUpdateActor {
  def props(moduleViewRepository: ModuleViewRepository, ctx: ExecutionContext) =
    Props(new ModuleCompendiumViewUpdateActor(moduleViewRepository, ctx))
}

private final class ModuleCompendiumViewUpdateActor(
    moduleViewRepository: ModuleViewRepository,
    implicit val ctx: ExecutionContext
) extends Actor
    with Logging {
  override def receive = {
    case CreatedOrUpdated(_, _) =>
      updateView()
    case Removed(_, _, _) =>
      updateView()
  }

  private def updateView(): Unit =
    moduleViewRepository.refreshView() onComplete {
      case Success(_) =>
        logger.info(s"successfully updated ${moduleViewRepository.name}")
      case Failure(e) =>
        logError(e)
    }

  private def logError(throwable: Throwable): Unit =
    logger.error(
      s"""failed to update ${moduleViewRepository.name}
         |  - message: ${throwable.getMessage}
         |  - trace: ${throwable.getStackTrace.mkString(
          "\n           "
        )}""".stripMargin
    )
}
