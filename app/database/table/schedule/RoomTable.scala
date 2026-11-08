package database.table.schedule

import java.util.UUID

import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper
import database.Schema
import models.schedule.Room
import slick.jdbc.PostgresProfile.api.*

private[database] final class RoomTable(tag: Tag) extends Table[Room](tag, Some(Schema.Schedule.name), "room") {

  def id = column[UUID]("id", O.PrimaryKey)

  def label = column[String]("label")

  def abbrev = column[String]("abbrev")

  def `type` = column[String]("type")

  def capacity = column[Int]("capacity")

  def examCapacityOpt = column[Option[Int]]("exam_capacity_opt")

  def examCapacityMax = column[Option[Int]]("exam_capacity_max")

  def seating = column[Option[String]]("seating")

  def pcs = column[Option[Int]]("pcs")

  def equipment = column[List[String]]("equipment")

  override def * =
    (
      id,
      label,
      abbrev,
      `type`,
      capacity,
      examCapacityOpt,
      examCapacityMax,
      seating,
      pcs,
      equipment
    ) <> (Room.apply.tupled, Room.unapply)
}
