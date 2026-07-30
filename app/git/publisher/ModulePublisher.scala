package git.publisher

import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

import git.publisher.ModulePublisher.NotifySubscribers
import git.subscriber.ModuleSubscribers
import git.GitFile
import git.GitFileContent
import logging.errorC
import logging.infoC
import logging.CorrelationId
import org.apache.pekko.actor.Actor
import play.api.Logging
import service.pipeline.MetadataPipeline
import service.pipeline.Print

final class ModulePublisher @Inject() (
    private val pipeline: MetadataPipeline,
    private val subscribers: ModuleSubscribers,
    private implicit val ctx: ExecutionContext
) extends Actor
    with Logging {

  override def receive = {
    case NotifySubscribers(changes, correlationId) =>
      given CorrelationId = correlationId
      val prints          = changes.map(a => Print(a._2.value))
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
              logger.infoC(s"module publisher ok count=${modules.size}")
            case Left(errs) =>
              logger.errorC(
                s"module publisher validation failed count=${errs.size} messages=${errs.map(_.getMessage).mkString(" | ")}"
              )
          }
        case Failure(t) =>
          logger.errorC("module publisher failed", t)
      }
  }
}

object ModulePublisher {
  case class NotifySubscribers(
      moduleFiles: List[(GitFile.ModuleFile, GitFileContent)],
      correlationId: CorrelationId
  )
}
