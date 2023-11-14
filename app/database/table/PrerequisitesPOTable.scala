package database.table

import slick.jdbc.PostgresProfile.api._

import java.util.UUID

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
  ) <> (PrerequisitesPODbEntry.tupled, PrerequisitesPODbEntry.unapply)
}
