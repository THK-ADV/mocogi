package database.table

import java.util.UUID

import slick.jdbc.PostgresProfile.api._

case class ModuleGlobalCriteriaDbEntry(
    module: UUID,
    globalCriteria: String
)

final class ModuleGlobalCriteriaTable(tag: Tag)
    extends Table[ModuleGlobalCriteriaDbEntry](
      tag,
      "module_global_criteria"
    ) {

  def module = column[UUID]("module", O.PrimaryKey)

  def globalCriteria = column[String]("global_criteria", O.PrimaryKey)

  override def * = (
    module,
    globalCriteria
  ) <> (ModuleGlobalCriteriaDbEntry.apply, ModuleGlobalCriteriaDbEntry.unapply)
}
