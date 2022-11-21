package git.subscriber

import akka.actor.{Actor, Props}
import git.ModuleCompendiumSubscribers.{Added, Modified, Removed}
import play.api.Logging
import publisher.{KafkaPublisher, Record}
import validator.Metadata

import scala.util.{Failure, Success}

object ModuleCompendiumPublishActor {
  def props(publisher: KafkaPublisher[Metadata]) = Props(
    new ModuleCompendiumPublishActor(publisher)
  )
}

private final class ModuleCompendiumPublishActor(
    publisher: KafkaPublisher[Metadata]
) extends Actor
    with Logging {

  override def receive = {
    case Added(_, _, result) =>
      result.foreach(mc => publish(Record("added", mc.metadata)))
    case Modified(_, _, result) =>
      result.foreach(mc => publish(Record("updated", mc.metadata)))
    case Removed(_, path) =>
      logger.error(
        s"""failed to publish metadata record
           |  - git path: ${path.value}
           |  - message: deleting metadata is currently not supported""".stripMargin
      )
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
