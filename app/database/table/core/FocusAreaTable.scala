package database.table.core

import database.table.IDLabelDescColumn
import models.core.FocusArea
import slick.jdbc.PostgresProfile.api._

final class FocusAreaTable(tag: Tag) extends Table[FocusArea](tag, "focus_area") with IDLabelDescColumn[FocusArea] {

  def studyProgram = column[String]("study_program")

  override def * = (
    id,
    studyProgram,
    deLabel,
    enLabel,
    deDesc,
    enDesc
  ) <> (FocusArea.apply.tupled, FocusArea.unapply)
}
