package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class POOptionalDbEntry(
    metadata: UUID,
    po: String,
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

  def metadataFk =
    foreignKey("metadata", metadata, TableQuery[MetadataTable])(_.id)

  def instanceOfFk =
    foreignKey("instance_of", instanceOf, TableQuery[MetadataTable])(_.id)

  def poFk =
    foreignKey("po", po, TableQuery[POTable])(_.abbrev)

  override def * = (
    metadata,
    po,
    instanceOf,
    partOfCatalog,
    recommendedSemester
  ) <> (POOptionalDbEntry.tupled, POOptionalDbEntry.unapply)
}
