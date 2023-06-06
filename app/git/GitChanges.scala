package git

import java.time.LocalDateTime

case class GitChanges[A](
    added: A,
    modified: A,
    removed: List[GitFilePath],
    commitId: String,
    timestamp: LocalDateTime
)

object GitChanges {
  def apply(modified: List[GitFilePath]): GitChanges[List[GitFilePath]] =
    GitChanges(Nil, modified, Nil, "-", LocalDateTime.now())
}
