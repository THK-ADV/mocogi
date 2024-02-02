package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class POOptionalDbEntry(
    id: UUID,
    metadata: UUID,
    po: String,
    specialization: Option[String],
    instanceOf: UUID,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)

final class POOptionalTable(tag: Tag)
    extends Table[POOptionalDbEntry](tag, "po_optional") {

  def fullPo = specialization.fold(po)(identity)

  def id = column[UUID]("id", O.PrimaryKey)

  def metadata = column[UUID]("metadata")

  def po = column[String]("po")

  def instanceOf = column[UUID]("instance_of")

  def partOfCatalog = column[Boolean]("part_of_catalog")

  def recommendedSemester = column[List[Int]]("recommended_semester")

  def specialization = column[Option[String]]("specialization")

  override def * = (
    id,
    metadata,
    po,
    specialization,
    instanceOf,
    partOfCatalog,
    recommendedSemester
  ) <> (POOptionalDbEntry.tupled, POOptionalDbEntry.unapply)
}
