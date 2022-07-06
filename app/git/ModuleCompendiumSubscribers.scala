package git

import akka.actor.ActorRef
import controllers.parameter.PrinterOutputFormat
import git.publisher.ModuleCompendiumPublisher.OnUpdate
import parsing.types.ModuleCompendium

import javax.inject.Singleton

@Singleton
case class ModuleCompendiumSubscribers(private val value: List[ActorRef]) {
  def onUpdate(
      changes: GitChanges[List[(GitFilePath, ModuleCompendium)]],
      outputFormat: PrinterOutputFormat
  ): Unit = value.foreach(_ ! OnUpdate(changes, outputFormat))
}
