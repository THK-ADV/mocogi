package database.table

import models.core.StudyFormType
import slick.jdbc.PostgresProfile.api._

final class StudyFormTypeTable(tag: Tag)
    extends Table[StudyFormType](tag, "study_form_type")
    with AbbrevLabelColumn[StudyFormType] {
  override def * = (
    abbrev,
    deLabel,
    enLabel
  ) <> (StudyFormType.tupled, StudyFormType.unapply)
}
