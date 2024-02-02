package database.table
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDateTime

case class ModuleCompendiumListDbEntry(
    fullPo: String,
    po: String,
    specialization: Option[String],
    studyProgram: String,
    semester: String,
    deUrl: String,
    enUrl: String,
    generated: LocalDateTime
)

final class ModuleCompendiumListTable(tag: Tag)
    extends Table[ModuleCompendiumListDbEntry](
      tag,
      "module_compendium_list"
    ) {

  def fullPo = column[String]("full_po", O.PrimaryKey)

  def po = column[String]("po")

  def specialization = column[Option[String]]("specialization")

  def studyProgram = column[String]("study_program")

  def semester = column[String]("semester")

  def deUrl = column[String]("de_url")

  def enUrl = column[String]("en_url")

  def generated = column[LocalDateTime]("generated")

  def studyProgramFk =
    foreignKey("study_program", studyProgram, TableQuery[StudyProgramTable])(
      _.id
    )

  override def * = (
    fullPo,
    po,
    specialization,
    studyProgram,
    semester,
    deUrl,
    enUrl,
    generated
  ) <> (ModuleCompendiumListDbEntry.tupled, ModuleCompendiumListDbEntry.unapply)
}
