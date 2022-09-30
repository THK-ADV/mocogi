package database.table

import basedata.AssessmentMethod
import slick.jdbc.PostgresProfile.api._

final class AssessmentMethodTable(tag: Tag)
    extends Table[AssessmentMethod](tag, "assessment_method") {

  def abbrev = column[String]("abbrev", O.PrimaryKey)

  def deLabel = column[String]("de_label")

  def enLabel = column[String]("en_label")

  override def * = (
    abbrev,
    deLabel,
    enLabel
  ) <> (AssessmentMethod.tupled, AssessmentMethod.unapply)
}
