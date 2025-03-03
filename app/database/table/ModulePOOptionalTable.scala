package database.table

import java.util.UUID

import slick.jdbc.PostgresProfile.api.*

case class ModulePOOptionalDbEntry(
    id: UUID,
    module: UUID,
    po: String,
    specialization: Option[String],
    instanceOf: UUID,
    partOfCatalog: Boolean,
    recommendedSemester: List[Int]
)

final class ModulePOOptionalTable(tag: Tag) extends Table[ModulePOOptionalDbEntry](tag, "module_po_optional") {

  import database.MyPostgresProfile.MyAPI.simpleIntListTypeMapper

  def fullPo = specialization.fold(po)(identity)

  def id = column[UUID]("id", O.PrimaryKey)

  def module = column[UUID]("module")

  def po = column[String]("po")

  def instanceOf = column[UUID]("instance_of")

  def partOfCatalog = column[Boolean]("part_of_catalog")

  def recommendedSemester = column[List[Int]]("recommended_semester")

  def specialization = column[Option[String]]("specialization")

  override def * = (
    id,
    module,
    po,
    specialization,
    instanceOf,
    partOfCatalog,
    recommendedSemester
  ) <> (ModulePOOptionalDbEntry.apply, ModulePOOptionalDbEntry.unapply)
}
