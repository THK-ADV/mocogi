package models.core

import auth.CampusId
import database.table.core.IdentityDbEntry
import models.EmploymentType
import monocle.Lens
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.Writes

sealed trait Identity {
  def id: String
  def kind: String
  def username: Option[String]
  def fullName: String
}

object Identity {
  val PersonKind  = "person"
  val GroupKind   = "group"
  val UnknownKind = "unknown"

  lazy val NN: Identity = Unknown("nn", "N.N.")

  case class Person(
      id: String,
      lastname: String,
      firstname: String,
      title: String,
      faculties: List[String],
      abbreviation: String,
      campusId: Option[String],
      isActive: Boolean,
      employmentType: EmploymentType,
      websiteUrl: Option[String],
  ) extends Identity {
    override val kind                     = PersonKind
    override def username: Option[String] = campusId

    def fullName: String = s"$firstname $lastname"

    def email: Option[String] = username.map(CampusId.apply(_).toMailAddress)

    def hasEmail: Boolean = username.isDefined
  }

  case class Group(id: String, label: String) extends Identity {
    override val kind                     = GroupKind
    override def username: Option[String] = None
    override def fullName: String         = label
  }

  case class Unknown(id: String, label: String) extends Identity {
    override val kind                     = UnknownKind
    override def username: Option[String] = None
    override def fullName: String         = label
  }

  def toPersonUnsafe(p: IdentityDbEntry): Person =
    Person(
      p.id,
      p.lastname.getOrElse(""),
      p.firstname.getOrElse(""),
      p.title,
      p.faculties.getOrElse(Nil),
      p.abbreviation.getOrElse(""),
      p.campusId,
      p.isActive,
      p.employmentType.getOrElse(EmploymentType.Unknown),
      p.websiteUrl
    )

  def fromDbEntry(db: IdentityDbEntry): Identity =
    db.kind match {
      case PersonKind =>
        toPersonUnsafe(db)
      case GroupKind =>
        Group(db.id, db.title)
      case UnknownKind =>
        Unknown(db.id, db.title)
    }

  def toDbEntry(identity: Identity): IdentityDbEntry =
    identity match {
      case Person(
            id,
            lastname,
            firstname,
            title,
            faculties,
            abbreviation,
            campusId,
            isActive,
            employmentType,
            websiteUrl
          ) =>
        IdentityDbEntry(
          id,
          Some(lastname),
          Some(firstname),
          title,
          Option.when(faculties.nonEmpty)(faculties),
          Some(abbreviation),
          campusId,
          isActive,
          PersonKind,
          Some(employmentType),
          websiteUrl
        )
      case Group(id, title) =>
        IdentityDbEntry(
          id,
          None,
          None,
          title,
          None,
          None,
          None,
          isActive = true,
          GroupKind,
          None,
          None
        )
      case Unknown(id, title) =>
        IdentityDbEntry(
          id,
          None,
          None,
          title,
          None,
          None,
          None,
          isActive = true,
          UnknownKind,
          None,
          None,
        )
    }

  private def unknownWrites: Writes[Unknown] =
    Json
      .writes[Unknown]
      .transform((js: JsObject) => js + ("kind" -> JsString(UnknownKind)))

  implicit def personWrites: Writes[Person] =
    Json
      .writes[Person]
      .transform((js: JsObject) => js + ("kind" -> JsString(PersonKind)))

  implicit def groupWrites: Writes[Group] =
    Json
      .writes[Group]
      .transform((js: JsObject) => js + ("kind" -> JsString(GroupKind)))

  implicit def writes: Writes[Identity] = {
    case single: Person =>
      personWrites.writes(single)
    case group: Group =>
      groupWrites.writes(group)
    case unknown: Unknown =>
      unknownWrites.writes(unknown)
  }

  def idLens =
    Lens[Identity, String](_.id)(id => {
      case p: Identity.Person  => p.copy(id = id)
      case g: Identity.Group   => g.copy(id = id)
      case u: Identity.Unknown => u.copy(id = id)
    })
}
