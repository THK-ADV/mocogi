package git

sealed trait GitCommitActionType {
  override def toString = this match {
    case GitCommitActionType.Create => "create"
    case GitCommitActionType.Delete => "delete"
    case GitCommitActionType.Update => "update"
  }
}

object GitCommitActionType {
  case object Create extends GitCommitActionType

  case object Delete extends GitCommitActionType

  case object Update extends GitCommitActionType
}
