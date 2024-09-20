package database.table

import java.util.UUID

import slick.jdbc.PostgresProfile.api._

case class ModuleECTSFocusAreaContributionDbEntry(
    module: UUID,
    focusArea: String,
    ectsValue: Double,
    deDesc: String,
    enDesc: String
)

final class ModuleECTSFocusAreaContributionTable(tag: Tag)
    extends Table[ModuleECTSFocusAreaContributionDbEntry](
      tag,
      "module_ects_focus_area_contribution"
    ) {

  def module = column[UUID]("module", O.PrimaryKey)

  def focusArea = column[String]("focus_area", O.PrimaryKey)

  def ectsValue = column[Double]("ects_value")

  def deDesc = column[String]("de_desc")

  def enDesc = column[String]("en_desc")

  override def * = (
    module,
    focusArea,
    ectsValue,
    deDesc,
    enDesc
  ) <> (ModuleECTSFocusAreaContributionDbEntry.apply, ModuleECTSFocusAreaContributionDbEntry.unapply)
}
