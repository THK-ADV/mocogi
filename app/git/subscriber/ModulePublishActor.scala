package git.subscriber

import akka.actor.{Actor, Props}
import git.subscriber.ModuleSubscribers.CreatedOrUpdated
import play.api.Logging
import publisher.{KafkaPublisher, Record}
import validator.Metadata

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
      case CreatedOrUpdated(entries) if entries.nonEmpty =>
        entries.foreach { case (_, mc, _) =>
          publish(Record("updated", mc.metadata))
        }
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
