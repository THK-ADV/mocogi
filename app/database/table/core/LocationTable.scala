package database.table.core

import database.table.IDLabelColumn
import models.core.ModuleLocation
import slick.jdbc.PostgresProfile.api._

final class LocationTable(tag: Tag) extends Table[ModuleLocation](tag, "location") with IDLabelColumn[ModuleLocation] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> (ModuleLocation.apply.tupled, ModuleLocation.unapply)
}
