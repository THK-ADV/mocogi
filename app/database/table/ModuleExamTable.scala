package database.table

import java.util.UUID

import database.Schema
import slick.jdbc.PostgresProfile.api.*

/** Current exam names of a live module. A `None` name denotes the default exam named after the module. */
private[database] final class ModuleExamTable(tag: Tag)
    extends Table[(UUID, Option[String])](tag, Some(Schema.Exams.name), "module_exam") {

  def module = column[UUID]("module")

  def name = column[Option[String]]("name")

  override def * = (module, name)
}
