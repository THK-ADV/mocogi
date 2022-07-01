package git

import controllers.PrinterOutputFormat
import git.ModuleCompendiumPublisher.Go
import parser.ParsingError
import parsing.ModuleCompendiumParser
import parsing.types.ModuleCompendium
import play.api.Logging

import javax.inject.{Inject, Singleton}

trait ModuleCompendiumPublisher {
  def parser: ModuleCompendiumParser
  val subscribers: ModuleCompendiumSubscribers

  def notifyAllObservers(
      changes: GitChanges[List[String]],
      outputFormat: PrinterOutputFormat
  ): Unit
}

object ModuleCompendiumPublisher {
  case class Go(
      changes: GitChanges[List[ModuleCompendium]],
      outputFormat: PrinterOutputFormat
  )
}

@Singleton
final class ModuleCompendiumPublisherImpl @Inject() (
    val parser: ModuleCompendiumParser,
    val subscribers: ModuleCompendiumSubscribers
) extends ModuleCompendiumPublisher
    with Logging {
  override def notifyAllObservers(
      changes: GitChanges[List[String]],
      outputFormat: PrinterOutputFormat
  ): Unit = {
    val parsedChanges = parse(changes)
    subscribers.value.foreach(s => s ! Go(parsedChanges, outputFormat))
  }

  private def parse(
      changes: GitChanges[List[String]]
  ): GitChanges[List[ModuleCompendium]] = {
    val (addedErr, added) =
      changes.added.partitionMap(s => parser.parser.parse(s)._1)
    addedErr.foreach(logError)
    val (modifiedErr, modified) =
      changes.modified.partitionMap(s => parser.parser.parse(s)._1)
    modifiedErr.foreach(logError)
    val (removedErr, removed) =
      changes.removed.partitionMap(s => parser.parser.parse(s)._1)
    removedErr.foreach(logError)
    GitChanges(
      added,
      modified,
      removed
    )
  }

  private def logError(e: ParsingError): Unit =
    logger.error(e.getMessage)
}
