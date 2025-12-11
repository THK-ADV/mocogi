package service

import java.time.LocalDate

import git.cli.ModuleGitCLI
import models.ModuleProtocol
import play.api.Logging

/**
 * Provides functionality to retrieve active modules from a Git repository's preview branch
 * based on their relation to a specified PO.
 */
final class ModulePreview(gitCli: ModuleGitCLI) extends Logging {

  /**
   * Retrieves all active modules from the preview branch of the Git repository that are related to the given PO.
   */
  def getAllFromPreviewByPO(po: String): Vector[ModuleProtocol] = {
    val (errs, previewModules) = gitCli.getAllModulesFromPreview()
    if errs.nonEmpty then {
      logger.error(s"Failed to parse some modules from preview branch. Errors: ${errs.mkString("\n")}")
    }
    previewModules.collect { case (m, _) if m.metadata.isActive && m.metadata.po.hasPORelation(po) => m }
  }

  /**
   * Retrieves all active modules from the preview branch of the Git repository that are related to the given PO.
   */
  def getAllFromPreviewByPOWithLastModified(po: String): Vector[(ModuleProtocol, LocalDate)] = {
    val (errs, previewModules) = gitCli.getAllModulesFromPreview()
    if errs.nonEmpty then {
      logger.error(s"Failed to parse some modules from preview branch. Errors: ${errs.mkString("\n")}")
    }
    previewModules.filter((m, _) => m.metadata.isActive && m.metadata.po.hasPORelation(po))
  }
}
