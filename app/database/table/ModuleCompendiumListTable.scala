package database.table
import models.ModuleCompendiumList
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDateTime

final class ModuleCompendiumListTable(tag: Tag)
    extends Table[ModuleCompendiumList.DB](
      tag,
      "module_compendium_list"
    ) {

  def poAbbrev = column[String]("po", O.PrimaryKey)

  def poNumber = column[Int]("po_number")

  def studyProgram = column[String]("study_program")

  def semester = column[String]("semester", O.PrimaryKey)

  def deUrl = column[String]("de_url")

  def enUrl = column[String]("en_url")

  def generated = column[LocalDateTime]("generated")

  def studyProgramFk =
    foreignKey("study_program", studyProgram, TableQuery[StudyProgramTable])(
      _.abbrev
    )

  override def * = (
    poAbbrev,
    poNumber,
    studyProgram,
    semester,
    deUrl,
    enUrl,
    generated
  ) <> (mapRow, unmapRow)

  private def mapRow: (
      (
          String,
          Int,
          String,
          String,
          String,
          String,
          LocalDateTime
      )
  ) => ModuleCompendiumList.DB = {
    case (
          poAbbrev,
          poNumber,
          studyProgram,
          semester,
          deUrl,
          enUrl,
          generated
        ) =>
      ModuleCompendiumList(
        poAbbrev,
        poNumber,
        studyProgram,
        semester,
        deUrl,
        enUrl,
        generated
      )
  }

  private def unmapRow(arg: ModuleCompendiumList.DB): Option[
    (
        String,
        Int,
        String,
        String,
        String,
        String,
        LocalDateTime
    )
  ] =
    Some(
      (
        arg.poAbbrev,
        arg.poNumber,
        arg.studyProgram,
        arg.semester,
        arg.deUrl,
        arg.enUrl,
        arg.generated
      )
    )
}
