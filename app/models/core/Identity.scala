package models.core

import database.table.core.IdentityDbEntry
import play.api.libs.json.{JsObject, JsString, Json, Writes}

sealed trait Identity {
  def id: String
  def kind: String
  def username: Option[String]
  def fullName: String
}

object Identity {
  val PersonKind = "person"
  val GroupKind = "group"
  val UnknownKind = "unknown"

  case class Person(
      id: String,
      lastname: String,
      firstname: String,
      title: String,
      faculties: List[Faculty],
      abbreviation: String,
      campusId: String,
      status: PersonStatus
  ) extends Identity {
    override val kind = PersonKind
    override def username: Option[String] =
      Option.when(campusId.nonEmpty)(campusId)

    def fullName: String = s"$firstname $lastname"

    def email: Option[String] = username.map(_ + "@th-koeln.de")
  }

  case class Group(id: String, label: String) extends Identity {
    override val kind = GroupKind
    override def username: Option[String] = None
    override def fullName: String = label
  }

  case class Unknown(id: String, label: String) extends Identity {
    override val kind = UnknownKind
    override def username: Option[String] = None
    override def fullName: String = label
  }

  def toPerson(p: IdentityDbEntry): Person =
    Person(
      p.id,
      p.lastname,
      p.firstname,
      p.title,
      Nil,
      p.abbreviation,
      p.campusId.get,
      p.status
    )

  def fromDbEntry(
      identity: IdentityDbEntry,
      faculties: List[Faculty]
  ): Identity =
    identity.kind match {
      case PersonKind =>
        Person(
          identity.id,
          identity.lastname,
          identity.firstname,
          identity.title,
          faculties,
          identity.abbreviation,
          identity.campusId.get,
          identity.status
        )
      case GroupKind =>
        Group(identity.id, identity.title)
      case UnknownKind =>
        Unknown(identity.id, identity.title)
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
            status
          ) =>
        IdentityDbEntry(
          id,
          lastname,
          firstname,
          title,
          faculties.map(_.id),
          abbreviation,
          Some(campusId),
          status,
          PersonKind
        )
      case Group(id, title) =>
        IdentityDbEntry(
          id,
          "",
          "",
          title,
          Nil,
          "",
          None,
          PersonStatus.Active,
          GroupKind
        )
      case Unknown(id, title) =>
        IdentityDbEntry(
          id,
          "",
          "",
          title,
          Nil,
          "",
          None,
          PersonStatus.Active,
          UnknownKind
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
}
