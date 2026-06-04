package models.schedule

import java.time.LocalDateTime
import java.util.UUID

import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes

case class PlanDraft(
    id: UUID,
    kind: PlanDraftKind,
    semester: String,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    publishedAt: Option[LocalDateTime]
)

case class PlanDraftProtocol(
    kind: PlanDraftKind,
    semester: String
)

enum PlanDraftKind(val id: String) {
  case Schedule extends PlanDraftKind("schedule")
  case Exam     extends PlanDraftKind("exam")
}

object PlanDraft {
  given Writes[PlanDraft] = Json.writes
}

object PlanDraftProtocol {
  given Reads[PlanDraftProtocol] = Json.reads
}

object PlanDraftKind {
  given Writes[PlanDraftKind] = Writes.of[String].contramap(_.id)
  given Reads[PlanDraftKind]  = Reads.of[String].map(apply)

  def apply(id: String): PlanDraftKind =
    id match {
      case "schedule" => Schedule
      case "exam"     => Exam
      case _          => throw new IllegalArgumentException(s"invalid plan draft kind: $id")
    }
}
