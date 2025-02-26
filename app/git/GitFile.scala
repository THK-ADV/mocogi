package git

import java.time.LocalDateTime
import java.util.UUID

sealed trait GitFile {
  def path: GitFilePath
  def status: GitFileStatus
}

object GitFile {
  case class ModuleFile(path: GitFilePath, id: UUID, status: GitFileStatus, lastModified: Option[LocalDateTime])
      extends GitFile
  case class CoreFile(path: GitFilePath, status: GitFileStatus)          extends GitFile
  case class ModuleCatalogFile(path: GitFilePath, status: GitFileStatus) extends GitFile
  case class Other(path: GitFilePath, status: GitFileStatus)             extends GitFile
}
