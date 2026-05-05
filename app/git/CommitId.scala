package git

import play.api.libs.json.Writes

case class CommitId(value: String) extends AnyVal {
  override def toString = value
}

object CommitId {
  val empty = CommitId("-")

  given Writes[CommitId] = Writes.of[String].contramap(_.value)
}
