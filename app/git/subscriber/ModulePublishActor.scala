package git.subscriber

import git.subscriber.ModuleSubscribers.Handle
import git.{GitFile, GitFileStatus}
import kafka.{KafkaPublisher, Topics}
import monocle.macros.GenLens
import ops.LoggerOps
import org.apache.pekko.actor.{Actor, Props}
import parsing.types.Module
import play.api.Logging

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ModulePublishActor {
  def props(serverUrl: String, ctx: ExecutionContext, topics: Topics[Module]) =
    Props(
      new Impl(serverUrl, ctx, topics)
    )

  private final class Impl(
      override val serverUrl: String,
      override implicit val ctx: ExecutionContext,
      topics: Topics[Module]
  ) extends Actor
      with Logging
      with LoggerOps
      with KafkaPublisher {

    private val producer =
      makeUUIDProducer[Module](Seq(topics.created, topics.updated))

    private def partitionByFileStatus(
        modules: Seq[(Module, GitFile.ModuleFile)]
    ) = {
      val created = ListBuffer.empty[Module]
      val updated = ListBuffer.empty[Module]
      val deleted = ListBuffer.empty[Module]

      modules.foreach { case (module, file) =>
        file.status match {
          case GitFileStatus.Added    => created += module
          case GitFileStatus.Modified => updated += module
          case GitFileStatus.Removed  => deleted += module
        }
      }

      (created.toList, updated.toList, deleted.toList)
    }

    override def receive = {
      case Handle(modules, _) if modules.nonEmpty =>
        val (created, updated, _) = partitionByFileStatus(modules)

        val res = for {
          created <- sendMany(
            producer,
            topics.created,
            GenLens[Module](_.metadata.id),
            created
          )
          updated <- sendMany(
            producer,
            topics.updated,
            GenLens[Module](_.metadata.id),
            updated
          )
        } yield (created, updated)

        res onComplete {
          case Success((created, updated)) =>
            if (created > 0) {
              logger.info(
                s"successfully sent $created records to topic ${topics.created}"
              )
            }
            if (updated > 0) {
              logger.info(
                s"successfully sent $updated records to topic ${topics.updated}"
              )
            }
          case Failure(e) =>
            logStackTrace(e)
        }
    }
  }
}
