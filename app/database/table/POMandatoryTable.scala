package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class POMandatoryDbEntry(
    id: UUID,
    metadata: UUID,
    po: String,
    specialization: Option[String],
    recommendedSemester: List[Int]
)

final class POMandatoryTable(tag: Tag)
    extends Table[POMandatoryDbEntry](tag, "po_mandatory") {

  def id = column[UUID]("id", O.PrimaryKey)

  def metadata = column[UUID]("metadata")

  def po = column[String]("po")

  def recommendedSemester = column[List[Int]]("recommended_semester")

  def specialization = column[Option[String]]("specialization")

  override def * = (
    id,
    metadata,
    po,
    specialization,
    recommendedSemester
  ) <> (POMandatoryDbEntry.tupled, POMandatoryDbEntry.unapply)
}
