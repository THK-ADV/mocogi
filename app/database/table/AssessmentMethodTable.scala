package database.table

import models.core.AssessmentMethod
import slick.jdbc.PostgresProfile.api._

final class AssessmentMethodTable(tag: Tag)
    extends Table[AssessmentMethod](tag, "assessment_method")
    with IDLabelColumn[AssessmentMethod] {

  override def * = (
    id,
    deLabel,
    enLabel
  ) <> ((AssessmentMethod.apply _).tupled, AssessmentMethod.unapply)
}
