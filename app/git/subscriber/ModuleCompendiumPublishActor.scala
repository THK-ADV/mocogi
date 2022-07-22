package git.subscriber

import akka.actor.{Actor, Props}
import git.publisher.ModuleCompendiumPublisher.OnUpdate
import ops.PrettyPrinter
import parsing.types.Metadata
import play.api.Logging
import publisher.{KafkaPublisher, Record}

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

  override def receive = { case OnUpdate(changes, _) =>
    val addedRecords = changes.added.map { case (_, mc) =>
      Record("added", mc.metadata)
    }
    val updatedRecords = changes.modified.map { case (_, mc) =>
      Record("updated", mc.metadata)
    }
    changes.removed.foreach { path =>
      logger.info(
        s"need to delete module compendium with path ${path.value}"
      )
    }

    publisher.publishComplete(addedRecords ::: updatedRecords) {
      case (record, res) =>
        res match {
          case Success(_) =>
            logger.info(
              s"""successfully published module compendium
               |  - record: ${PrettyPrinter.prettyPrint(record)}""".stripMargin
            )
          case Failure(t) =>
            logger.error(
              s"""failed to publish module compendium
                 |  - record: $record
                 |  - message: ${t.getMessage}
                 |  - trace: ${t.getStackTrace.mkString(
                  "\n           "
                )}""".stripMargin
            )
        }
    }
  }
}
