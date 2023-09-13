package database.table

import models.{ModuleReviewer, ModuleReviewerRole, User}
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

final class ModuleReviewerTable(tag: Tag)
    extends Table[ModuleReviewer](
      tag,
      "module_reviewer"
    ) {

  def id = column[UUID]("id", O.PrimaryKey)

  def user = column[User]("user_id")

  def role = column[ModuleReviewerRole]("role")

  def studyProgram = column[String]("study_program")

  def studyProgramFk =
    foreignKey("studyProgram", studyProgram, TableQuery[StudyProgramTable])(
      _.abbrev
    )

  override def * = (
    id,
    user,
    role,
    studyProgram
  ) <> (ModuleReviewer.tupled, ModuleReviewer.unapply)
}
