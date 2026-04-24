package git.publisher

import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

import git.publisher.ModulePublisher.NotifySubscribers
import git.subscriber.ModuleSubscribers
import git.GitFile
import git.GitFileContent
import logging.AppEventLogger
import logging.CorrelationId
import logging.LogEvent
import logging.LogResult
import org.apache.pekko.actor.Actor
import play.api.Logging
import service.*
import service.pipeline.MetadataPipeline
import service.pipeline.PipelineError
import service.pipeline.Print

final class ModulePublisher @Inject() (
    private val pipeline: MetadataPipeline,
    private val subscribers: ModuleSubscribers,
    private implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  override def receive = {
    case NotifySubscribers(changes, correlationId) =>
      val event  = "module.publisher.notify_subscribers"
      val prints = changes.map(a => Print(a._2.value))
      infoEvent(
        event = event,
        result = LogResult.Started,
        correlationId = correlationId,
        details = Map("changeCount" -> changes.size.toString)
      )
      pipeline.parseValidateMany(prints).onComplete {
        case Success(validates) =>
          val modules = validates.map(_.map {
            case (_, module) =>
              val m = module.normalized()
              val f = changes.find(_._1.id == m.metadata.id).get._1
              (m, f)
          })
          modules match {
            case Right(modules) =>
              subscribers.handle(modules, correlationId)
              infoEvent(
                event = event,
                result = LogResult.Succeeded,
                correlationId = correlationId,
                details = Map("moduleCount" -> modules.size.toString)
              )
            case Left(errs) =>
              logPipelineErrors(event, correlationId, errs)
          }
        case Failure(t) =>
          logFutureFailure(event, correlationId, t)
      }
  }

  private def logPipelineErrors(event: String, correlationId: CorrelationId, errs: Seq[PipelineError]): Unit =
    AppEventLogger.error(
      logger,
      LogEvent(
        event = event,
        result = LogResult.Failed,
        correlationId = correlationId,
        errorCode = Some("module_pipeline_validation_failed"),
        details = Map("errorCount" -> errs.size.toString, "messages" -> errs.map(_.getMessage).mkString(" | "))
      )
    )

  private def logFutureFailure(event: String, correlationId: CorrelationId, t: Throwable): Unit =
    AppEventLogger.error(
      logger,
      LogEvent(
        event = event,
        result = LogResult.Failed,
        correlationId = correlationId,
        errorCode = Some("module_publisher_failed")
      ),
      t
    )

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

object ModulePublisher {
  case class NotifySubscribers(
      moduleFiles: List[(GitFile.ModuleFile, GitFileContent)],
      correlationId: CorrelationId
  )
}
