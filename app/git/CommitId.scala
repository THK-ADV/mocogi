package git

case class CommitId(value: String) extends AnyVal {
  override def toString = value
}

object CommitId {
  val empty = CommitId("-")
}
