package git

case class GitCommitAction(
    action: GitCommitActionType,
    filePath: GitFilePath,
    fileContent: String
)
