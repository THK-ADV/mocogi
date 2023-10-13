package models

case class CommitId(value: String) extends AnyVal

object CommitId {
  val empty = CommitId("-")
}
