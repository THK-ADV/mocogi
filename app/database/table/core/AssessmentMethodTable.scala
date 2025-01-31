package database.table.core

import database.table.IDLabelColumn
import models.AssessmentMethodSource
import slick.jdbc.PostgresProfile.api.*

case class AssessmentMethodDbEntry(id: String, deLabel: String, enLabel: String, source: AssessmentMethodSource)

final class AssessmentMethodTable(tag: Tag)
    extends Table[AssessmentMethodDbEntry](tag, "assessment_method")
    with IDLabelColumn[AssessmentMethodDbEntry] {

  import database.table.given_BaseColumnType_AssessmentMethodSource

  def source = column[AssessmentMethodSource]("source")

  override def * = (
    id,
    deLabel,
    enLabel,
    source
  ) <> (AssessmentMethodDbEntry.apply, AssessmentMethodDbEntry.unapply)
}
