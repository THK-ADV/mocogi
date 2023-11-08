package git

import models.CommitId

import java.time.LocalDateTime

case class GitChanges[A](
    added: A,
    modified: A,
    removed: List[GitFilePath],
    commitId: CommitId,
    timestamp: LocalDateTime
)

object GitChanges {
  def apply(
      modified: List[(GitFilePath, GitFileContent)]
  ): GitChanges[List[(GitFilePath, GitFileContent)]] =
    GitChanges(Nil, modified, Nil, CommitId.empty, LocalDateTime.now())
}
