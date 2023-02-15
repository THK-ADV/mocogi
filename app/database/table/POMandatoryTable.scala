package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class POMandatoryDbEntry(
    metadata: UUID,
    po: String,
    recommendedSemester: List[Int],
    recommendedPartTimeSemester: List[Int]
)

final class POMandatoryTable(tag: Tag)
    extends Table[POMandatoryDbEntry](tag, "po_mandatory") {

  def metadata = column[UUID]("metadata", O.PrimaryKey)

  def po = column[String]("po", O.PrimaryKey)

  def recommendedSemester =
    column[List[Int]]("recommended_semester")

  def recommendedPartTimeSemester =
    column[List[Int]]("recommended_semester_part_time")

  def metadataFk =
    foreignKey("metadata", metadata, TableQuery[ModuleCompendiumTable])(_.id)

  def poFk =
    foreignKey("po", po, TableQuery[POTable])(_.abbrev)

  override def * = (
    metadata,
    po,
    recommendedSemester,
    recommendedPartTimeSemester
  ) <> (POMandatoryDbEntry.tupled, POMandatoryDbEntry.unapply)
}
