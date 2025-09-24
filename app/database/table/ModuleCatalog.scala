package database.table
import java.time.LocalDateTime

import slick.jdbc.PostgresProfile.api._

case class ModuleCatalogEntry(
    fullPo: String,
    po: String,
    specialization: Option[String],
    studyProgram: String,
    semester: String,
    url: String,
    generated: LocalDateTime
)

final class ModuleCatalog(tag: Tag)
    extends Table[ModuleCatalogEntry](
      tag,
      "module_catalog"
    ) {

  def fullPo = column[String]("full_po", O.PrimaryKey)

  def po = column[String]("po")

  def specialization = column[Option[String]]("specialization")

  def studyProgram = column[String]("study_program")

  def semester = column[String]("semester")

  def deUrl = column[String]("de_url")

  def enUrl = column[String]("en_url")

  def generated = column[LocalDateTime]("generated")

  override def * = (
    fullPo,
    po,
    specialization,
    studyProgram,
    semester,
    deUrl,
    generated
  ) <> (ModuleCatalogEntry.apply, ModuleCatalogEntry.unapply)
}
