package git

import models.CommitId

import java.time.LocalDateTime
import scala.collection.mutable.ListBuffer

case class GitChanges[A](
    added: A,
    modified: A,
    removed: List[GitFilePath],
    commitId: CommitId,
    timestamp: LocalDateTime
)

object GitChanges {

  case class CategorizedGitFilePaths(
      modules: List[GitFilePath],
      core: List[GitFilePath],
      mcs: List[GitFilePath]
  )

  def apply(
      modified: List[(GitFilePath, GitFileContent)]
  ): GitChanges[List[(GitFilePath, GitFileContent)]] =
    GitChanges(Nil, modified, Nil, CommitId.empty, LocalDateTime.now())

  final implicit class ListOps(val self: GitChanges[List[GitFilePath]])
      extends AnyVal {
    def categorized(implicit config: GitConfig): CategorizedGitFilePaths = {
      val modules = ListBuffer.empty[GitFilePath]
      val core = ListBuffer.empty[GitFilePath]
      val mcs = ListBuffer.empty[GitFilePath]
      (self.added ::: self.modified).foreach { p =>
        if (p.isModule) {
          modules += p
        } else if (p.isCore) {
          core += p
        } else if (p.isModuleCompendium) {
          mcs += p
        }
      }
      CategorizedGitFilePaths(modules.toList, core.toList, mcs.toList)
    }
  }
}
