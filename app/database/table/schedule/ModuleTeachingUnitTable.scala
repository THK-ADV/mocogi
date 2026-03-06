package database.table.schedule

import java.util.UUID

import database.Schema
import models.schedule.ModuleTeachingUnit
import slick.jdbc.PostgresProfile.api.*

private[database] final class ModuleTeachingUnitTable(tag: Tag)
    extends Table[ModuleTeachingUnit](tag, Some(Schema.Schedule.name), "module_teaching_unit") {

  import database.MyPostgresProfile.MyAPI.simpleUUIDListTypeMapper

  def module = column[UUID]("module", O.PrimaryKey)

  def teachingUnits = column[List[UUID]]("teaching_units")

  override def * = (module, teachingUnits) <> (ModuleTeachingUnit.apply.tupled, ModuleTeachingUnit.unapply)
}
