package git

import java.time.LocalDateTime

case class GitChanges[A](
    entries: A,
    commitId: CommitId,
    timestamp: LocalDateTime
)
