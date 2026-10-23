package git

import java.util.UUID

import scala.util.control.NonFatal

import models.ModuleDraft

sealed trait GitFilePath extends Any {
  def value: String
  override def toString = value
}

object GitFilePath {
  private case class GitFilePathImpl(value: String) extends AnyVal with GitFilePath

  private def modulePrefix(implicit config: GitRepositoryConfig) = s"${config.modulesPath}/"

  private def moduleFileExt = ".md"

  private def catalogFileExt = ".tex"

  def apply(path: String): GitFilePath = GitFilePathImpl(path)

  def apply(draft: ModuleDraft)(implicit config: GitRepositoryConfig): GitFilePath = apply(draft.module)

  def apply(moduleId: UUID)(implicit config: GitRepositoryConfig): GitFilePath =
    apply(s"$modulePrefix${moduleId.toString}$moduleFileExt")

  def moduleCompanionPath(moduleId: UUID, po: String)(implicit config: GitRepositoryConfig): GitFilePath =
    apply(s"${config.moduleCompanionsPath}/${moduleId.toString}_$po$moduleFileExt")

  implicit class Ops(private val self: GitFilePath) extends AnyVal {
    def moduleId(implicit config: GitRepositoryConfig): Option[UUID] = {
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

    def isModule(implicit config: GitRepositoryConfig): Boolean =
      self.value.startsWith(config.modulesPath) && self.value.endsWith(
        moduleFileExt
      )

    def isModuleCatalog(implicit config: GitRepositoryConfig): Boolean =
      self.value.startsWith(config.moduleCatalogsPath) && self.value
        .endsWith(catalogFileExt)

    def fold[A](module: UUID => A, catalog: => A, other: => A)(
        implicit config: GitRepositoryConfig
    ): A =
      self.moduleId match {
        case Some(id) =>
          module(id)
        case None =>
          if (self.isModuleCatalog) catalog
          else other
      }
  }
}
