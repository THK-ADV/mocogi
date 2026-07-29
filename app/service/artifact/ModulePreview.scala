package service.artifact

import java.time.LocalDate
import java.util.UUID

import scala.util.Try

import cli.GitCLI
import models.ModuleProtocol
import play.api.Logging

/**
 * All active modules of a PO, taken from the preview branch. Children are kept separate from
 * [[modules]] because they are rendered inside their parent and usually carry no PO assignment
 * themselves.
 */
private[artifact] case class POModules(
    modules: Vector[(ModuleProtocol, LocalDate)],
    children: Vector[(ModuleProtocol, LocalDate)]
) {
  lazy val childrenById: Map[UUID, ModuleProtocol] =
    children.flatMap((module, _) => module.id.map(_ -> module)).toMap

  /** The children of the given subset of [[modules]]. */
  def childrenOf(modules: Vector[ModuleProtocol]): Vector[(ModuleProtocol, LocalDate)] = {
    val childIds = modules.flatMap(_.metadata.childIds).toSet
    children.filter((child, _) => child.id.exists(childIds.contains))
  }
}

/**
 * Provides functionality to retrieve active modules from a Git repository's preview branch
 * based on their relation to a specified PO.
 */
private[artifact] final class ModulePreview(gitCli: GitCLI) extends Logging {

  def getByPO(po: String): Try[POModules] =
    gitCli.getAllModulesFromPreview().map { (errs, previewModules) =>
      if errs.nonEmpty then {
        logger.error(s"Failed to parse some modules from preview branch. Errors: ${errs.mkString("\n")}")
      }
      val active                          = previewModules.filter((module, _) => module.metadata.isActive)
      val directlyAssigned                = active.filter((module, _) => module.metadata.po.hasPORelation(po))
      val childIds                        = directlyAssigned.flatMap((module, _) => module.metadata.childIds).toSet
      def isChild(module: ModuleProtocol) = module.id.exists(childIds.contains)

      POModules(
        modules = directlyAssigned.filterNot((module, _) => isChild(module)),
        children = active.filter((module, _) => isChild(module))
      )
    }
}
