package git

case class GitChanges[A](
    entries: A,
    commitId: CommitId
)
