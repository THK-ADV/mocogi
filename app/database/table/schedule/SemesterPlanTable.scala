package database.table.schedule

import database.Schema

import java.time.LocalDate
import java.util.UUID
import models.schedule.SemesterPlan
import models.schedule.SemesterPlanType
import slick.jdbc.PostgresProfile.api.*

private[database] final class SemesterPlanTable(tag: Tag)
    extends Table[SemesterPlan](tag, Some(Schema.Schedule.name), "semester_plan") {

  import database.MyPostgresProfile.MyAPI.simpleIntListTypeMapper

  given BaseColumnType[SemesterPlanType] =
    MappedColumnType.base[SemesterPlanType, String](_.id, SemesterPlanType.apply)

  def id = column[UUID]("id", O.PrimaryKey)

  def start = column[LocalDate]("start")

  def end = column[LocalDate]("end")

  def kind = column[SemesterPlanType]("type")

  def teachingUnit = column[Option[UUID]]("teaching_unit")

  def semesterIndex = column[Option[List[Int]]]("semester_index")

  def phase = column[Option[String]]("phase")

  override def * = (
    id,
    start,
    end,
    kind,
    teachingUnit,
    semesterIndex,
    phase,
  ) <> (SemesterPlan.apply.tupled, SemesterPlan.unapply)
}
