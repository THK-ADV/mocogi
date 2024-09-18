package database.table
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDateTime

case class ModuleCatalogEntry(
    fullPo: String,
    po: String,
    specialization: Option[String],
    studyProgram: String,
    semester: String,
    deUrl: String,
    enUrl: String,
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
    enUrl,
    generated
  ) <> (ModuleCatalogEntry.apply, ModuleCatalogEntry.unapply)
}
