package models

case class CommitId(value: String)

object CommitId {
  val empty = CommitId("-")
}
