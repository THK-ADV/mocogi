package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class ModulePOMandatoryDbEntry(
    id: UUID,
    module: UUID,
    po: String,
    specialization: Option[String],
    recommendedSemester: List[Int]
)

final class ModulePOMandatoryTable(tag: Tag)
    extends Table[ModulePOMandatoryDbEntry](tag, "module_po_mandatory") {

  def id = column[UUID]("id", O.PrimaryKey)

  def module = column[UUID]("module")

  def po = column[String]("po")

  def recommendedSemester = column[List[Int]]("recommended_semester")

  def specialization = column[Option[String]]("specialization")

  override def * = (
    id,
    module,
    po,
    specialization,
    recommendedSemester
  ) <> (ModulePOMandatoryDbEntry.apply, ModulePOMandatoryDbEntry.unapply)
}
