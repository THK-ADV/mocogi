package git

import git.GitFilesBroker.{Changes, core, modules, split}
import git.publisher.{CoreDataPublisher, ModuleCompendiumPublisher}

import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer

trait GitFilesBroker {
  def distributeToSubscriber(changes: Changes): Unit
}

object GitFilesBroker {
  val modules = "modules"
  val core = "core"

  type Changes = GitChanges[List[(GitFilePath, GitFileContent)]]

  def split(changes: Changes): Map[String, Changes] = {
    val addedModules = ListBuffer[(GitFilePath, GitFileContent)]()
    val addedCore = ListBuffer[(GitFilePath, GitFileContent)]()
    val modifiedModules = ListBuffer[(GitFilePath, GitFileContent)]()
    val modifiedCore = ListBuffer[(GitFilePath, GitFileContent)]()
    val removedModules = ListBuffer[GitFilePath]()
    val removedCore = ListBuffer[GitFilePath]()

    changes.added.foreach(a =>
      foldGitFilePath(a._1)(p => addedModules += p -> a._2)(p =>
        addedCore += p -> a._2
      )
    )
    changes.modified.foreach(a =>
      foldGitFilePath(a._1)(p => modifiedModules += p -> a._2)(p =>
        modifiedCore += p -> a._2
      )
    )
    changes.removed.foreach(a =>
      foldGitFilePath(a)(p => removedModules += p)(p => removedCore += p)
    )

    Map(
      modules -> changes.copy(
        added = addedModules.toList,
        modified = modifiedModules.toList,
        removed = removedModules.toList
      ),
      core -> changes.copy(
        added = addedCore.toList,
        modified = modifiedCore.toList,
        removed = removedCore.toList
      )
    ).filterNot(a => isEmpty(a._2))
  }

  def isEmpty(changes: Changes) =
    changes.added.isEmpty && changes.modified.isEmpty && changes.removed.isEmpty

  def foldGitFilePath(
      path: GitFilePath
  )(isModule: GitFilePath => Unit)(isCore: GitFilePath => Unit): Unit =
    if (path.value.startsWith(s"$modules/")) isModule(path)
    else if (path.value.startsWith(s"$core/")) isCore(path)
}

@Singleton
final class GitFilesBrokerImpl @Inject() (
    private val moduleCompendiumParsingValidator: ModuleCompendiumPublisher,
    private val coreDataParsingValidator: CoreDataPublisher
    // val parsingValidators: Map[GitFilePath, List[ParsingValidator[_]]],
) extends GitFilesBroker {
  override def distributeToSubscriber(changes: Changes): Unit = {
    val map = split(changes)
    map
      .get(modules)
      .foreach(moduleCompendiumParsingValidator.notifySubscribers)
    map
      .get(core)
      .foreach(coreDataParsingValidator.notifySubscribers)
  }
}
