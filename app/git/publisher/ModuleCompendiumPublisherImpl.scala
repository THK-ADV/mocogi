package git.publisher

import controllers.parameter.PrinterOutputFormat
import git.publisher.ModuleCompendiumPublisher.OnUpdate
import git.{GitChanges, GitFileContent, GitFilePath, ModuleCompendiumSubscribers}
import ops.EitherOps._
import parser.ParsingError
import parsing.ModuleCompendiumParser
import parsing.types.ModuleCompendium
import play.api.Logging

import javax.inject.{Inject, Singleton}

@Singleton
final class ModuleCompendiumPublisherImpl @Inject() (
    val parser: ModuleCompendiumParser,
    val subscribers: ModuleCompendiumSubscribers
) extends ModuleCompendiumPublisher
    with Logging {
  override def notifyAllObservers(
      changes: GitChanges[List[(GitFilePath, GitFileContent)]],
      outputFormat: PrinterOutputFormat
  ): Unit = {
    val parsedChanges = parse(changes)
    subscribers.value.foreach(s => s ! OnUpdate(parsedChanges, outputFormat))
  }

  private def parse(
      changes: GitChanges[List[(GitFilePath, GitFileContent)]]
  ): GitChanges[List[ModuleCompendium]] = {
    val logErr = logError(changes.commitId) _

    def go(xs: List[(GitFilePath, GitFileContent)]) = {
      val (errs, parserRes) =
        xs.partitionMap(t =>
          parser.parser.parse(t._2.value)._1.mapLeft(_ -> t._1)
        )
      errs.foreach(logErr)
      parserRes
    }

    changes.copy(
      go(changes.added),
      go(changes.modified),
      go(changes.removed)
    )
  }

  private def logError(
      commitId: String
  )(t: (ParsingError, GitFilePath)): Unit = {
    val error = t._1
    val filePath = t._2.value
    val msg = s"""parsing error occurred
       | - commit id: $commitId
       | - file: $filePath
       | - expected: ${error.expected}
       | - found: ${error.found.take(100)}...""".stripMargin
    logger.error(msg)
  }
}
