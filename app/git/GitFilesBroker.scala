package git

import controllers.parameter.PrinterOutputFormat
import git.GitFilesBroker.{Changes, core, modules, split}
import git.publisher.{CoreDataParsingValidator, ModuleCompendiumPublisher}
import validator.Validation

import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

trait ParsingValidator[A] {
  def parse(input: String): Future[Validation[A]]
}

trait GitFilesBroker {
  // def parsingValidators: Map[GitFilePath, List[ParsingValidator[_]]]
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
      modules -> GitChanges(
        addedModules.toList,
        modifiedModules.toList,
        removedModules.toList,
        changes.commitId
      ),
      core -> GitChanges(
        addedCore.toList,
        modifiedCore.toList,
        removedCore.toList,
        changes.commitId
      )
    ).filterNot(a => isEmpty(a._2))
  }

  def isEmpty(changes: Changes) =
    changes.added.isEmpty && changes.modified.isEmpty && changes.removed.isEmpty

  def foldGitFilePath(
      path: GitFilePath
  )(isModule: GitFilePath => Unit)(isCore: GitFilePath => Unit): Unit =
    if (path.value.startsWith(s"$modules/"))
      isModule(path.copy(path.value.stripPrefix(s"$modules/")))
    else if (path.value.startsWith(s"$core/"))
      isCore(path.copy(path.value.stripPrefix(s"$core/")))
}

@Singleton
final class GitFilesBrokerImpl @Inject() (
    private val publisher: ModuleCompendiumPublisher,
    private val coreDataParsingValidator: CoreDataParsingValidator
    // val parsingValidators: Map[GitFilePath, List[ParsingValidator[_]]],
) extends GitFilesBroker {
  override def distributeToSubscriber(changes: Changes): Unit = {
    val map = split(changes)
    map
      .get(modules)
      .foreach(changes =>
        publisher.notifySubscribers(changes, PrinterOutputFormat.DefaultPrinter)
      )
    map.get(core).foreach(coreDataParsingValidator.notifySubscribers)
  }
}
