package database.table
import models.ModuleCompendiumList
import slick.jdbc.PostgresProfile.api._

import java.time.LocalDateTime

final class ModuleCompendiumListTable(tag: Tag)
    extends Table[ModuleCompendiumList.DB](
      tag,
      "module_compendium_list"
    ) {

  def fullPoId = column[String]("full_po", O.PrimaryKey)

  def poId = column[String]("po")

  def poNumber = column[Int]("po_number")

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
    fullPoId,
    poId,
    poNumber,
    specialization,
    studyProgram,
    semester,
    deUrl,
    enUrl,
    generated
  ) <> (mapRow, unmapRow)

  private def mapRow: (
      (
          String,
          String,
          Int,
          Option[String],
          String,
          String,
          String,
          String,
          LocalDateTime
      )
  ) => ModuleCompendiumList.DB = {
    case (
          fullPoId,
          poId,
          poNumber,
          specialization,
          studyProgram,
          semester,
          deUrl,
          enUrl,
          generated
        ) =>
      ModuleCompendiumList(
        fullPoId,
        poId,
        poNumber,
        specialization,
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
        String,
        Int,
        Option[String],
        String,
        String,
        String,
        String,
        LocalDateTime
    )
  ] =
    Some(
      (
        arg.fullPoId,
        arg.poId,
        arg.poNumber,
        arg.specialization,
        arg.studyProgram,
        arg.semester,
        arg.deUrl,
        arg.enUrl,
        arg.generated
      )
    )
}
