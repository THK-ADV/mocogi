package database.table

import basedata.StudyProgramPreview
import slick.jdbc.PostgresProfile.api._

final class StudyProgramTable(tag: Tag)
    extends Table[StudyProgramPreview](tag, "status") {

  def abbrev = column[String]("abbrev", O.PrimaryKey)

  override def * =
    abbrev <> (StudyProgramPreview.apply, StudyProgramPreview.unapply)
}
