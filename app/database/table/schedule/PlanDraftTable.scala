package database.table.schedule

import java.time.LocalDateTime
import java.util.UUID

import database.Schema
import models.schedule.PlanDraft
import models.schedule.PlanDraftKind
import slick.jdbc.PostgresProfile.api.*

private[database] final class PlanDraftTable(tag: Tag)
    extends Table[PlanDraft](tag, Some(Schema.Schedule.name), "plan_draft") {

  import database.table.given_BaseColumnType_PlanDraftKind

  def id = column[UUID]("id", O.PrimaryKey)

  def kind = column[PlanDraftKind]("kind")

  /**
   * Semester.id is used as the semester column value.
   */
  def semester = column[String]("semester")

  def createdAt = column[LocalDateTime]("created_at")

  def updatedAt = column[LocalDateTime]("updated_at")

  /**
   * `None` = active draft (editable). `Some` = published and immutable
   */
  def publishedAt = column[Option[LocalDateTime]]("published_at")

  override def * = (
    id,
    kind,
    semester,
    createdAt,
    updatedAt,
    publishedAt,
  ) <> (PlanDraft.apply.tupled, PlanDraft.unapply)
}
