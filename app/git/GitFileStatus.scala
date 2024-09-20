package git

sealed trait GitFileStatus {
  def isRemoved: Boolean =
    this == GitFileStatus.Removed
}

object GitFileStatus {
  case object Added    extends GitFileStatus
  case object Modified extends GitFileStatus
  case object Removed  extends GitFileStatus
}
