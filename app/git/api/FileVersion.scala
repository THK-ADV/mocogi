package git.api

import java.time.LocalDateTime

import git.CommitId
import git.GitFileContent

final case class FileVersion(
    commitId: CommitId,
    committedAt: LocalDateTime,
    content: Option[GitFileContent]
)
