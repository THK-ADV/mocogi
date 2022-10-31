package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class ECTSFocusAreaContributionDbEntry(
    id: UUID,
    ects: UUID,
    focusArea: UUID,
    ectsValue: Double,
    description: String
)

final class ECTSFocusAreaContributionTable(tag: Tag)
    extends Table[ECTSFocusAreaContributionDbEntry](
      tag,
      "ects_focus_area_contribution"
    ) {

  def id = column[UUID]("id", O.PrimaryKey)

  def ects = column[UUID]("ects")

  def focusArea = column[UUID]("focus_area")

  def ectsValue = column[Double]("ects_value")

  def description = column[String]("description")

  override def * = (
    id,
    ects,
    focusArea,
    ectsValue,
    description
  ) <> (ECTSFocusAreaContributionDbEntry.tupled, ECTSFocusAreaContributionDbEntry.unapply)
}
