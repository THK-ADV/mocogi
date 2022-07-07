package git.publisher

import akka.actor.{Actor, ActorRef, Props}
import controllers.parameter.PrinterOutputFormat
import git.publisher.ModuleCompendiumPublisher.NotifySubscribers
import git.{
  GitChanges,
  GitFileContent,
  GitFilePath,
  ModuleCompendiumSubscribers
}
import ops.EitherOps.EOps
import parser.ParsingError
import parsing.ModuleCompendiumParser
import parsing.types.ModuleCompendium
import play.api.Logging

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ModuleCompendiumPublisher {
  def props(
      parser: ModuleCompendiumParser,
      subscribers: ModuleCompendiumSubscribers,
      ctx: ExecutionContext
  ) = Props(new ModuleCompendiumPublisherImpl(parser, subscribers, ctx))

  case class OnUpdate(
      changes: GitChanges[List[(GitFilePath, ModuleCompendium)]],
      outputFormat: PrinterOutputFormat
  )

  private final class ModuleCompendiumPublisherImpl(
      private val parser: ModuleCompendiumParser,
      private val subscribers: ModuleCompendiumSubscribers,
      private implicit val ctx: ExecutionContext
  ) extends Actor
      with Logging {
    override def receive = { case NotifySubscribers(changes, outputFormat) =>
      parse(changes) onComplete {
        case Success(parsedChanges) =>
          subscribers.onUpdate(parsedChanges, outputFormat)
        case Failure(t) =>
          logThrowable(t)
      }
    }

    private def parse(
        changes: GitChanges[List[(GitFilePath, GitFileContent)]]
    ): Future[GitChanges[List[(GitFilePath, ModuleCompendium)]]] =
      parser.parser().map { p =>
        val logErr = logError(changes.commitId) _

        def go(
            xs: List[(GitFilePath, GitFileContent)]
        ): List[(GitFilePath, ModuleCompendium)] = {
          val (errs, parserRes) = xs.partitionMap(t =>
            p.parse(t._2.value)
              ._1
              .map(t._1 -> _)
              .mapLeft(t._1 -> _)
          )

          errs.foreach(logErr)
          parserRes
        }

        changes.copy(
          go(changes.added),
          go(changes.modified)
        )
      }

    private def logThrowable(t: Throwable): Unit =
      logger.error(
        s"""failed to parse
           |  - message: ${t.getMessage}
           |  - trace: ${t.getStackTrace.mkString(
            "\n           "
          )}""".stripMargin
      )

    private def logError(
        commitId: String
    )(t: (GitFilePath, ParsingError)): Unit = {
      val error = t._2
      val filePath = t._1.value
      logger.error(
        s"""parsing error occurred
           | - commit id: $commitId
           | - file: $filePath
           | - expected: ${error.expected}
           | - found: ${error.found.take(100)}...""".stripMargin
      )
    }
  }

  private case class NotifySubscribers(
      changes: GitChanges[List[(GitFilePath, GitFileContent)]],
      outputFormat: PrinterOutputFormat
  )
}

@Singleton
case class ModuleCompendiumPublisher(private val value: ActorRef) {
  def notifySubscribers(
      changes: GitChanges[List[(GitFilePath, GitFileContent)]],
      outputFormat: PrinterOutputFormat
  ): Unit =
    value ! NotifySubscribers(changes, outputFormat)
}
