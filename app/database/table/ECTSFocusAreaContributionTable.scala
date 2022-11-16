package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class ECTSFocusAreaContributionDbEntry(
    metadata: UUID,
    focusArea: String,
    ectsValue: Double,
    description: String
)

final class ECTSFocusAreaContributionTable(tag: Tag)
    extends Table[ECTSFocusAreaContributionDbEntry](
      tag,
      "ects_focus_area_contribution"
    ) {

  def metadata = column[UUID]("metadata", O.PrimaryKey)

  def focusArea = column[String]("focus_area", O.PrimaryKey)

  def ectsValue = column[Double]("ects_value")

  def description = column[String]("description")

  override def * = (
    metadata,
    focusArea,
    ectsValue,
    description
  ) <> (ECTSFocusAreaContributionDbEntry.tupled, ECTSFocusAreaContributionDbEntry.unapply)
}
