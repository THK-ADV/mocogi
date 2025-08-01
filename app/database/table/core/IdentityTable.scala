package database.table.core

import models.core.Identity
import models.EmploymentType
import slick.jdbc.PostgresProfile.api.*

// TODO reorder
case class IdentityDbEntry(
    id: String,
    lastname: Option[String],
    firstname: Option[String],
    title: String,
    faculties: Option[List[String]],
    abbreviation: Option[String],
    campusId: Option[String],
    isActive: Boolean,
    kind: String,
    employmentType: Option[EmploymentType],
    websiteUrl: Option[String],
) {
  def isPerson = this.kind == Identity.PersonKind
}

final class IdentityTable(tag: Tag) extends Table[IdentityDbEntry](tag, "identity") {

  import database.MyPostgresProfile.MyAPI.simpleStrListTypeMapper

  given BaseColumnType[EmploymentType] =
    MappedColumnType.base[EmploymentType, String](_.id, EmploymentType.apply)

  def id = column[String]("id", O.PrimaryKey)

  def lastname = column[Option[String]]("lastname")

  def firstname = column[Option[String]]("firstname")

  def title = column[String]("title")

  def faculties = column[Option[List[String]]]("faculties")

  def abbreviation = column[Option[String]]("abbreviation")

  def campusId = column[Option[String]]("campus_id")

  def isActive = column[Boolean]("is_active")

  def kind = column[String]("kind")

  def employmentType = column[Option[EmploymentType]]("employment_type")

  def websiteUrl = column[Option[String]]("website_url")

  def isPerson: Rep[Boolean] =
    this.kind === Identity.PersonKind

  override def * = (
    id,
    lastname,
    firstname,
    title,
    faculties,
    abbreviation,
    campusId,
    isActive,
    kind,
    employmentType,
    websiteUrl
  ) <> (IdentityDbEntry.apply, IdentityDbEntry.unapply)
}
