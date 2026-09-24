package database.table

import java.time.LocalDate

import database.Schema
import slick.jdbc.PostgresProfile.api.*

private[database] case class ModuleCatalogDbEntry(po: String, semester: String, date: LocalDate, url: String)

private[database] final class ModuleCatalogTable(tag: Tag)
    extends Table[ModuleCatalogDbEntry](tag, Some(Schema.Modules.name), "module_catalog") {

  def po = column[String]("po", O.PrimaryKey)

  def semester = column[String]("semester")

  def date = column[LocalDate]("date")

  def url = column[String]("url")

  override def * = (po, semester, date, url) <> (ModuleCatalogDbEntry.apply, ModuleCatalogDbEntry.unapply)
}
