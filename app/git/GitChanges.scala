package git

case class GitChanges[A](
    added: A,
    modified: A,
    removed: A,
    commitId: String
)
