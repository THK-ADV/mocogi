package git.publisher

import akka.actor.{Actor, ActorRef, Props}
import git.GitFilesBroker.Changes
import git.publisher.ModuleCompendiumPublisher.NotifySubscribers
import git.subscriber.ModuleCompendiumSubscribers
import play.api.Logging
import service._

import javax.inject.Singleton
import scala.concurrent.ExecutionContext

object ModuleCompendiumPublisher {
  def props(
      metadataParsingService: MetadataParsingService,
      moduleCompendiumService: ModuleCompendiumService,
      subscribers: ModuleCompendiumSubscribers,
      ctx: ExecutionContext
  ) = Props(
    new ModuleCompendiumPublisherImpl(
      metadataParsingService,
      moduleCompendiumService,
      subscribers,
      ctx
    )
  )

  private final class ModuleCompendiumPublisherImpl(
      private val parsingService: MetadataParsingService,
      moduleCompendiumService: ModuleCompendiumService,
      private val subscribers: ModuleCompendiumSubscribers,
      private implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {

    override def receive = { case NotifySubscribers(changes) =>
//      go(changes)
    }

//    private def go(changes: Changes): Unit = { // TODO
//      val allChanges = changes.added ++ changes.modified
//      val allPrints =
//        allChanges.map(c => (Option.empty[UUID], Print(c._2.value)))
//      val f = for {
//        parsed <- parsingService.parseMany(allPrints)
//        validates <- continue(parsed, validatingService.validateMany)
//      } yield validates.map(_.map { case (print, mc) =>
//        (allChanges.find(_._2.value == print.value).get._1, mc.normalize())
//      })
//
//      f onComplete {
//        case Success(s) =>
//          s match {
//            case Right(mcs) =>
//              subscribers.createdOrUpdated(
//                mcs.map(t => (t._1, t._2, changes.timestamp))
//              )
//            case Left(errs) => logPipelineErrors(errs)
//          }
//        case Failure(t) =>
//          logFutureFailure(t)
//      }
//    }

    private def logPipelineErrors(errs: Seq[PipelineError]): Unit =
      logger.error(
        s"""failed to parse or validate module compendium
           |  - messages: ${errs
            .map(_.getMessage)
            .mkString("\n")}""".stripMargin
      )

    private def logFutureFailure(t: Throwable): Unit =
      logger.error(
        s"""failed to handle git push event
           |  - message: ${t.getMessage}
           |  - trace: ${t.getStackTrace.mkString(
            "\n           "
          )}""".stripMargin
      )
  }
  private case class NotifySubscribers(changes: Changes)
}

@Singleton
case class ModuleCompendiumPublisher(private val value: ActorRef) {
  def notifySubscribers(changes: Changes): Unit =
    value ! NotifySubscribers(changes)
}
