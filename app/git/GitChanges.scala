package git

case class GitChanges[A](
    added: A,
    modified: A,
    removed: List[GitFilePath],
    commitId: String
)
