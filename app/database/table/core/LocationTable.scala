package database.table.core

import database.table.IDLabelColumn
import database.Schema
import models.core.ModuleLocation
import slick.jdbc.PostgresProfile.api.*

private[database] final class LocationTable(tag: Tag)
    extends Table[ModuleLocation](tag, Some(Schema.Core.name), "location")
    with IDLabelColumn[ModuleLocation] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> (ModuleLocation.apply.tupled, ModuleLocation.unapply)
}
