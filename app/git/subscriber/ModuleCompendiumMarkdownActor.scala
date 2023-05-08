package git.subscriber

import akka.actor.{Actor, ActorRef, Props}
import git.subscriber.ModuleCompendiumMarkdownActor.Convert
import play.api.Logging
import printing.pandoc.{PandocApi, PrinterOutput, PrinterOutputType}

import java.util.UUID
import javax.inject.Singleton

object ModuleCompendiumMarkdownActor {
  def props(
      markdownConverter: PandocApi,
      outputType: PrinterOutputType
  ) = Props(
    new ModuleCompendiumMarkdownActorImpl(markdownConverter, outputType)
  )

  private case class Convert(
      title: String,
      moduleId: UUID,
      input: String,
      path: String
  )

  private class ModuleCompendiumMarkdownActorImpl(
      private val markdownConverter: PandocApi,
      private val outputType: PrinterOutputType
  ) extends Actor
      with Logging {
    override def receive: Receive = {
      case Convert(title, moduleId, input, path) =>
        markdownConverter.run(moduleId, outputType, input, path) match {
          case Left(err) =>
            logError(title, moduleId, err)
          case Right(output) =>
            output match {
              case PrinterOutput.Text(content, _, consoleOutput) =>
                logText(title, moduleId, content, consoleOutput)
              case PrinterOutput.File(path, consoleOutput) =>
                logFile(title, moduleId, path, consoleOutput)
            }
        }
    }

    private def logError(title: String, id: UUID, t: Throwable): Unit =
      logger.error(
        s"""failed to convert module compendium to $outputType
           |  - title: $title
           |  - id: $id
           |  - message: ${t.getMessage}
           |  - trace: ${t.getStackTrace.mkString(
            "\n           "
          )}""".stripMargin
      )

    private def logText(
        title: String,
        id: UUID,
        content: String,
        consoleOutput: String
    ): Unit =
      logger.info(
        s"""successfully converted module compendium to $outputType
           |  - title: $title
           |  - id: $id
           |  - content: ${content.length}
           |  - console output: $consoleOutput""".stripMargin
      )

    private def logFile(
        title: String,
        id: UUID,
        path: String,
        consoleOutput: String
    ): Unit =
      logger.info(
        s"""successfully converted module compendium to $outputType
           |  - title: $title
           |  - id: $id
           |  - path: $path
           |  - console output: $consoleOutput""".stripMargin
      )
  }
}

@Singleton
case class ModuleCompendiumMarkdownActor(private val value: ActorRef) {
  def convert(
      title: String,
      moduleId: UUID,
      input: String,
      path: String
  ): Unit =
    value ! Convert(title, moduleId, input, path)
}
