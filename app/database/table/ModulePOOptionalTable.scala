package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class ModulePOOptionalDbEntry(
    id: UUID,
    module: UUID,
    po: String,
    specialization: Option[String],
    instanceOf: Option[UUID],
    focus: Boolean,
    recommendedSemester: List[Int]
)

final class ModulePOOptionalTable(tag: Tag)
    extends Table[ModulePOOptionalDbEntry](tag, "module_po_optional") {

  def fullPo = specialization.fold(po)(identity)

  def id = column[UUID]("id", O.PrimaryKey)

  def module = column[UUID]("module")

  def po = column[String]("po")

  def instanceOf = column[Option[UUID]]("instance_of")

  def focus = column[Boolean]("focus")

  def recommendedSemester = column[List[Int]]("recommended_semester")

  def specialization = column[Option[String]]("specialization")

  override def * = (
    id,
    module,
    po,
    specialization,
    instanceOf,
    focus,
    recommendedSemester
  ) <> (ModulePOOptionalDbEntry.tupled, ModulePOOptionalDbEntry.unapply)
}
