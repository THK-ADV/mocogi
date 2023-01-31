package git

case class GitCommitAction(
    action: GitCommitActionType,
    filename: String,
    fileContent: String
)
