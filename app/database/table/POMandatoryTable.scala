package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class POMandatoryDbEntry(
    metadata: UUID,
    po: String,
    specialization: Option[String],
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

  def specialization =
    column[Option[String]]("specialization")

  override def * = (
    metadata,
    po,
    specialization,
    recommendedSemester,
    recommendedPartTimeSemester
  ) <> (POMandatoryDbEntry.tupled, POMandatoryDbEntry.unapply)
}
