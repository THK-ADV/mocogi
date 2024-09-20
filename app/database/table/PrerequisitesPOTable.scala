package database.table

import java.util.UUID

import slick.jdbc.PostgresProfile.api._

case class PrerequisitesPODbEntry(
    prerequisites: UUID,
    po: String
)

final class PrerequisitesPOTable(tag: Tag)
    extends Table[PrerequisitesPODbEntry](
      tag,
      "prerequisites_po"
    ) {

  def prerequisites = column[UUID]("prerequisites", O.PrimaryKey)

  def po = column[String]("po", O.PrimaryKey)

  override def * = (
    prerequisites,
    po
  ) <> (PrerequisitesPODbEntry.apply, PrerequisitesPODbEntry.unapply)
}
