package git

import models.ModuleDraft

import java.util.UUID

sealed trait GitFilePath {
  def value: String
}

object GitFilePath {
  private case class GitFilePathImpl(value: String) extends GitFilePath

  def apply(path: String): GitFilePath =
    GitFilePathImpl(path)

  def apply(draft: ModuleDraft)(implicit gitConfig: GitConfig): GitFilePath =
    apply(draft.module)

  def apply(moduleId: UUID)(implicit gitConfig: GitConfig): GitFilePath =
    apply(s"${gitConfig.modulesRootFolder}/${moduleId.toString}.md")
}
