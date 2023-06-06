package git

import git.GitFilesBroker.{Changes, split}
import git.publisher.{CoreDataPublisher, ModuleCompendiumPublisher}

import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer

trait GitFilesBroker {
  def distributeToSubscriber(changes: Changes): Unit
}

object GitFilesBroker {
  type Changes = GitChanges[List[(GitFilePath, GitFileContent)]]

  def split(
      changes: Changes
  )(implicit config: GitConfig): Map[String, Changes] = {
    val addedModules = ListBuffer[(GitFilePath, GitFileContent)]()
    val addedCore = ListBuffer[(GitFilePath, GitFileContent)]()
    val modifiedModules = ListBuffer[(GitFilePath, GitFileContent)]()
    val modifiedCore = ListBuffer[(GitFilePath, GitFileContent)]()
    val removedModules = ListBuffer[GitFilePath]()
    val removedCore = ListBuffer[GitFilePath]()

    changes.added.foreach(a =>
      foldGitFilePath(
        a._1,
        p => addedModules += p -> a._2,
        p => addedCore += p -> a._2
      )
    )
    changes.modified.foreach(a =>
      foldGitFilePath(
        a._1,
        p => modifiedModules += p -> a._2,
        p => modifiedCore += p -> a._2
      )
    )
    changes.removed.foreach(a =>
      foldGitFilePath(a, p => removedModules += p, p => removedCore += p)
    )

    Map(
      config.modulesRootFolder -> changes.copy(
        added = addedModules.toList,
        modified = modifiedModules.toList,
        removed = removedModules.toList
      ),
      config.coreRootFolder -> changes.copy(
        added = addedCore.toList,
        modified = modifiedCore.toList,
        removed = removedCore.toList
      )
    ).filterNot(a => isEmpty(a._2))
  }

  def isEmpty(changes: Changes) =
    changes.added.isEmpty && changes.modified.isEmpty && changes.removed.isEmpty

  private def foldGitFilePath(
      path: GitFilePath,
      isModule: GitFilePath => Unit,
      isCore: GitFilePath => Unit
  )(implicit config: GitConfig): Unit =
    if (path.value.startsWith(s"${config.modulesRootFolder}/")) isModule(path)
    else if (path.value.startsWith(s"${config.coreRootFolder}/")) isCore(path)
}

@Singleton
final class GitFilesBrokerImpl @Inject() (
    private val moduleCompendiumPublisher: ModuleCompendiumPublisher,
    private val coreDataPublisher: CoreDataPublisher,
    private implicit val gitConfig: GitConfig
) extends GitFilesBroker {
  override def distributeToSubscriber(changes: Changes): Unit = {
    val map = split(changes)
    map
      .get(gitConfig.modulesRootFolder)
      .foreach(moduleCompendiumPublisher.notifySubscribers)
    map
      .get(gitConfig.coreRootFolder)
      .foreach(coreDataPublisher.notifySubscribers)
  }
}
