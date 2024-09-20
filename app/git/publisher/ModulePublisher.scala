package git.publisher

import java.time.LocalDateTime
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

import git.publisher.ModulePublisher.NotifySubscribers
import git.subscriber.ModuleSubscribers
import git.GitFile
import git.GitFileContent
import org.apache.pekko.actor.Actor
import org.apache.pekko.actor.ActorRef
import org.apache.pekko.actor.Props
import play.api.Logging
import service._

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

    override def receive = {
      case NotifySubscribers(changes, lastModified) =>
        val prints = changes.map(a => Print(a._2.value))
        pipeline.parseValidateMany(prints).onComplete {
          case Success(validates) =>
            val modules = validates.map(_.map {
              case (_, module) =>
                val m = module.normalize()
                val f = changes.find(_._1.id == m.metadata.id).get._1
                (m, f)
            })
            modules match {
              case Right(modules) =>
                subscribers.handle(modules, lastModified)
              case Left(errs) =>
                logPipelineErrors(errs)
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
      moduleFiles: List[(GitFile.ModuleFile, GitFileContent)],
      lastModified: LocalDateTime
  )
}

@Singleton
case class ModulePublisher(private val value: ActorRef) {
  def notifySubscribers(
      moduleFiles: List[(GitFile.ModuleFile, GitFileContent)],
      lastModified: LocalDateTime
  ): Unit =
    value ! NotifySubscribers(moduleFiles, lastModified)
}
