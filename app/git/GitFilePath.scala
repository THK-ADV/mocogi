package git

import models.ModuleDraft

import java.util.UUID
import scala.util.Try

sealed trait GitFilePath extends Any {
  def value: String
  override def toString = value
}

object GitFilePath {
  private case class GitFilePathImpl(value: String)
      extends AnyVal
      with GitFilePath

  private def modulePrefix(implicit gitConfig: GitConfig) =
    s"${gitConfig.modulesRootFolder}/"

  private def moduleFileExt = ".md"

  private def coreFileExt = ".yaml"

  private def mcsFileExt = ".tex"

  def apply(path: String): GitFilePath =
    GitFilePathImpl(path)

  def apply(draft: ModuleDraft)(implicit gitConfig: GitConfig): GitFilePath =
    apply(draft.module)

  def apply(moduleId: UUID)(implicit gitConfig: GitConfig): GitFilePath =
    apply(s"$modulePrefix${moduleId.toString}$moduleFileExt")

  implicit class Ops(private val self: GitFilePath) extends AnyVal {
    def moduleId(implicit gitConfig: GitConfig): Option[UUID] = {
      val prefix = modulePrefix
      val suffix = moduleFileExt
      if (self.value.startsWith(prefix) && self.value.endsWith(suffix)) {
        Try(
          UUID.fromString(
            self.value.stripPrefix(prefix).stripSuffix(suffix)
          )
        ).toOption
      } else {
        None
      }
    }

    def isModule(implicit gitConfig: GitConfig): Boolean =
      self.value.startsWith(gitConfig.modulesRootFolder) && self.value.endsWith(
        moduleFileExt
      )

    def isCore(implicit gitConfig: GitConfig): Boolean =
      self.value.startsWith(gitConfig.coreRootFolder) && self.value.endsWith(
        coreFileExt
      )

    def isModuleCompendium(implicit gitConfig: GitConfig): Boolean =
      self.value.startsWith(gitConfig.moduleCompendiumRootFolder) && self.value
        .endsWith(mcsFileExt)
  }
}
