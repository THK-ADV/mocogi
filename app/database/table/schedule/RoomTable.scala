package database.table.schedule

import java.util.UUID

import database.Schema
import models.schedule.Room
import slick.jdbc.PostgresProfile.api.*

private[database] final class RoomTable(tag: Tag) extends Table[Room](tag, Some(Schema.Schedule.name), "room") {

  def id = column[UUID]("id", O.PrimaryKey)

  def label = column[String]("label")

  def abbrev = column[String]("abbrev")

  def `type` = column[String]("type")

  def capacity = column[Int]("capacity")

  override def * = (id, label, abbrev, `type`, capacity) <> (Room.apply.tupled, Room.unapply)
}
