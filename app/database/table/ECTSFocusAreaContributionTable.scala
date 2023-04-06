package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class ECTSFocusAreaContributionDbEntry(
    metadata: UUID,
    focusArea: String,
    ectsValue: Double,
    deDesc: String,
    enDesc: String
)

final class ECTSFocusAreaContributionTable(tag: Tag)
    extends Table[ECTSFocusAreaContributionDbEntry](
      tag,
      "ects_focus_area_contribution"
    ) {

  def metadata = column[UUID]("metadata", O.PrimaryKey)

  def focusArea = column[String]("focus_area", O.PrimaryKey)

  def ectsValue = column[Double]("ects_value")

  def deDesc = column[String]("de_desc")

  def enDesc = column[String]("en_desc")

  override def * = (
    metadata,
    focusArea,
    ectsValue,
    deDesc,
    enDesc
  ) <> (ECTSFocusAreaContributionDbEntry.tupled, ECTSFocusAreaContributionDbEntry.unapply)
}
