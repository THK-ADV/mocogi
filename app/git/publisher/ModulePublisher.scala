package git.publisher

import akka.actor.{Actor, ActorRef, Props}
import git.publisher.ModulePublisher.NotifySubscribers
import git.subscriber.ModuleSubscribers
import git.{GitChanges, GitFileContent, GitFilePath}
import play.api.Logging
import service._

import java.util.UUID
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ModulePublisher {
  def props(
      pipeline: MetadataPipeline,
      subscribers: ModuleSubscribers,
      ctx: ExecutionContext
  ) = Props(
    new Impl(
      pipeline,
      subscribers,
      ctx
    )
  )

  private final class Impl(
      private val pipeline: MetadataPipeline,
      private val subscribers: ModuleSubscribers,
      private implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {

    override def receive = { case NotifySubscribers(changes) =>
      go(changes)
    }

    private def go(
        changes: GitChanges[List[(GitFilePath, GitFileContent)]]
    ): Unit = {
      val allChanges = changes.added ++ changes.modified
      val allPrints =
        allChanges.map(c => (Option.empty[UUID], Print(c._2.value)))

      pipeline.parseValidateMany(allPrints) onComplete {
        case Success(validates) =>
          val res = validates.map(_.map { case (print, mc) =>
            (allChanges.find(_._2.value == print.value).get._1, mc.normalize())
          })

          res match {
            case Right(mcs) =>
              subscribers.createdOrUpdated(
                mcs.map(t => (t._1, t._2, changes.timestamp))
              )
            case Left(errs) => logPipelineErrors(errs)
          }
        case Failure(t) =>
          logFutureFailure(t)
      }
    }

    private def logPipelineErrors(errs: Seq[PipelineError]): Unit =
      logger.error(
        s"""failed to parse or validate module
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
  private case class NotifySubscribers(
      changes: GitChanges[List[(GitFilePath, GitFileContent)]]
  )
}

@Singleton
case class ModulePublisher(private val value: ActorRef) {
  def notifySubscribers(
      changes: GitChanges[List[(GitFilePath, GitFileContent)]]
  ): Unit =
    value ! NotifySubscribers(changes)
}
