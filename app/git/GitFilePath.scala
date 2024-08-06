package git

import models.ModuleDraft

import java.util.UUID
import scala.util.control.NonFatal

sealed trait GitFilePath extends Any {
  def value: String
  override def toString = value
}

object GitFilePath {
  private case class GitFilePathImpl(value: String)
      extends AnyVal
      with GitFilePath

  private def modulePrefix(implicit gitConfig: GitConfig) =
    s"${gitConfig.modulesFolder}/"

  private def moduleFileExt = ".md"

  private def coreFileExt = ".yaml"

  private def catalogFileExt = ".tex"

  def apply(path: String): GitFilePath =
    GitFilePathImpl(path)

  def apply(draft: ModuleDraft)(implicit gitConfig: GitConfig): GitFilePath =
    apply(draft.module)

  def apply(moduleId: UUID)(implicit gitConfig: GitConfig): GitFilePath =
    apply(s"$modulePrefix${moduleId.toString}$moduleFileExt")

  implicit class Ops(private val self: GitFilePath) extends AnyVal {
    def fileName =
      self.value.slice(
        self.value.lastIndexOf("/") + 1,
        self.value.lastIndexOf(".")
      )

    def moduleId(implicit gitConfig: GitConfig): Option[UUID] = {
      val prefix = modulePrefix
      val suffix = moduleFileExt
      if (self.value.startsWith(prefix) && self.value.endsWith(suffix)) {
        try {
          Some(
            UUID.fromString(
              self.value.stripPrefix(prefix).stripSuffix(suffix)
            )
          )
        } catch {
          case NonFatal(_) => None
        }
      } else {
        None
      }
    }

    def isModule(implicit gitConfig: GitConfig): Boolean =
      self.value.startsWith(gitConfig.modulesFolder) && self.value.endsWith(
        moduleFileExt
      )

    def isCore(implicit gitConfig: GitConfig): Boolean =
      self.value.startsWith(gitConfig.coreFolder) && self.value.endsWith(
        coreFileExt
      )

    def isModuleCatalog(implicit gitConfig: GitConfig): Boolean =
      self.value.startsWith(gitConfig.moduleCatalogsFolder) && self.value
        .endsWith(catalogFileExt)

    def fold[A](module: UUID => A, core: => A, catalog: => A, other: => A)(
        implicit gitConfig: GitConfig
    ): A =
      self.moduleId match {
        case Some(id) =>
          module(id)
        case None =>
          if (self.isCore) core
          else if (self.isModuleCatalog) catalog
          else other
      }
  }
}
