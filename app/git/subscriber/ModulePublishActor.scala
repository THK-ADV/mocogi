package git.subscriber

import akka.actor.{Actor, Props}
import git.subscriber.ModuleSubscribers.CreatedOrUpdated
import models.Metadata
import play.api.Logging
import publisher.{KafkaPublisher, Record}

import scala.annotation.unused
import scala.util.{Failure, Success}

@unused
object ModulePublishActor {
  @unused
  def props(publisher: KafkaPublisher[Metadata]) = Props(
    new Impl(publisher)
  )

  private final class Impl(
      publisher: KafkaPublisher[Metadata]
  ) extends Actor
      with Logging {

    override def receive = {
      case CreatedOrUpdated(modules, _) if modules.nonEmpty =>
        modules.foreach(module => publish(Record("updated", module.metadata)))
    }

    private def publish(record: Record[Metadata]): Unit =
      publisher.publishComplete(Seq(record)) { case (record, res) =>
        res match {
          case Success(_) =>
            logger.info(
              s"""successfully published metadata record
                 |  - id: ${record.value.id}
                 |  - abbrev: ${record.value.abbrev}""".stripMargin
            )
          case Failure(t) =>
            logger.error(
              s"""failed to publish metadata record
                 |  - id: ${record.value.id}
                 |  - abbrev: ${record.value.abbrev}
                 |  - message: ${t.getMessage}
                 |  - trace: ${t.getStackTrace.mkString(
                  "\n           "
                )}""".stripMargin
            )
        }
      }
  }
}
