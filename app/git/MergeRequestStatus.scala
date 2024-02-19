package git

sealed trait MergeRequestStatus {
  def id: String
  override def toString = id
}

object MergeRequestStatus {
  case object Open extends MergeRequestStatus {
    override def id: String = "open"
  }
  case object Closed extends MergeRequestStatus {
    override def id: String = "closed"
  }
  case object Merged extends MergeRequestStatus {
    override def id: String = "merged"
  }

  def apply(id: String) =
    id match {
      case "open"   => Open
      case "closed" => Closed
      case "merged" => Merged
    }
}
