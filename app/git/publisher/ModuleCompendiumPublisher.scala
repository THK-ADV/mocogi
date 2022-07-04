package git.publisher

import controllers.parameter.PrinterOutputFormat
import git.{GitChanges, GitFileContent, GitFilePath, ModuleCompendiumSubscribers}
import parsing.ModuleCompendiumParser
import parsing.types.ModuleCompendium

trait ModuleCompendiumPublisher {
  def parser: ModuleCompendiumParser
  val subscribers: ModuleCompendiumSubscribers

  def notifySubscribers(
      changes: GitChanges[List[(GitFilePath, GitFileContent)]],
      outputFormat: PrinterOutputFormat
  ): Unit
}

object ModuleCompendiumPublisher {
  case class OnUpdate(
      changes: GitChanges[List[ModuleCompendium]],
      outputFormat: PrinterOutputFormat
  )
}
