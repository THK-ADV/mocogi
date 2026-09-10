package database.table.core

import database.table.IDLabelColumn
import database.Schema
import models.core.AssessmentMethodDefinition
import models.AssessmentMethodSource
import slick.jdbc.PostgresProfile.api.*

private[database] final class AssessmentMethodTable(tag: Tag)
    extends Table[AssessmentMethodDefinition](tag, Some(Schema.Core.name), "assessment_method")
    with IDLabelColumn[AssessmentMethodDefinition] {

  import database.table.given_BaseColumnType_AssessmentMethodSource

  def source = column[AssessmentMethodSource]("source")

  override def * = (
    id,
    deLabel,
    enLabel,
    source
  ) <> (AssessmentMethodDefinition.apply, AssessmentMethodDefinition.unapply)
}
