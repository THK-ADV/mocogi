package database.table

import models.core.StudyFormType
import slick.jdbc.PostgresProfile.api._

final class StudyFormTypeTable(tag: Tag)
    extends Table[StudyFormType](tag, "study_form_type")
    with IDLabelColumn[StudyFormType] {
  override def * = (
    id,
    deLabel,
    enLabel
  ) <> ((StudyFormType.apply _).tupled, StudyFormType.unapply)
}
