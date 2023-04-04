package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class POOptionalDbEntry(
    metadata: UUID,
    po: String,
    specialization: Option[String],
    instanceOf: UUID,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)

final class POOptionalTable(tag: Tag)
    extends Table[POOptionalDbEntry](tag, "po_optional") {

  def metadata = column[UUID]("metadata", O.PrimaryKey)

  def po = column[String]("po", O.PrimaryKey)

  def instanceOf = column[UUID]("instance_of")

  def partOfCatalog = column[Boolean]("part_of_catalog")

  def recommendedSemester =
    column[List[Int]]("recommended_semester", O.PrimaryKey)

  def specialization =
    column[Option[String]]("specialization")

  override def * = (
    metadata,
    po,
    specialization,
    instanceOf,
    partOfCatalog,
    recommendedSemester
  ) <> (POOptionalDbEntry.tupled, POOptionalDbEntry.unapply)
}
