package git

import java.time.LocalDateTime

case class GitChanges[A](
    added: A,
    modified: A,
    removed: List[GitFilePath],
    commitId: String,
    timestamp: LocalDateTime
)
