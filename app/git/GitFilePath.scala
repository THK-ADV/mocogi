package git

import models.ValidModuleDraft

import java.util.UUID

sealed trait GitFilePath {
  def value: String
}

object GitFilePath {
  private case class GitFilePathImpl(value: String) extends GitFilePath

  def apply(path: String): GitFilePath =
    GitFilePathImpl(path)

  def apply(
      draft: ValidModuleDraft
  )(implicit gitConfig: GitConfig): GitFilePath = {
    val path = s"${gitConfig.modulesRootFolder}/${draft.module.toString}.md"
    GitFilePathImpl(path)
  }

  def apply(
      existingPaths: Seq[(UUID, GitFilePath)],
      draft: ValidModuleDraft
  )(implicit gitConfig: GitConfig): GitFilePath = {
    val path = existingPaths.find(_._1 == draft.module) match {
      case Some((_, path)) => path.value
      case None => s"${gitConfig.modulesRootFolder}/${draft.module.toString}.md"
    }
    GitFilePathImpl(path)
  }
}
