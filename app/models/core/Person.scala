package models.core

import database.table.PersonDbEntry

sealed trait Person {
  def id: String
  def kind: String
  def username: Option[String]
  def fullName: String
}

object Person {
  val DefaultKind = "default"
  val GroupKind = "group"
  val UnknownKind = "unknown"

  case class Default(
      id: String,
      lastname: String,
      firstname: String,
      title: String,
      faculties: List[Faculty],
      abbreviation: String,
      campusId: String,
      status: PersonStatus
  ) extends Person {
    override val kind = DefaultKind
    override def username: Option[String] =
      Option.when(campusId.nonEmpty)(campusId)

    def fullName: String = s"$firstname $lastname"

    def email: Option[String] = username.map(a => s"$a@th-koeln.de")
  }

  case class Group(id: String, label: String) extends Person {
    override val kind = GroupKind
    override def username: Option[String] = None
    override def fullName: String = label
  }

  case class Unknown(id: String, label: String) extends Person {
    override val kind = UnknownKind
    override def username: Option[String] = None
    override def fullName: String = label
  }

  // unsafe
  def toDefaultPerson(p: PersonDbEntry) =
    Person.Default(
      p.id,
      p.lastname,
      p.firstname,
      p.title,
      Nil,
      p.abbreviation,
      p.campusId.get,
      p.status
    )

  def fromDbEntry(p: PersonDbEntry, faculties: List[Faculty]): Person =
    p.kind match {
      case Person.DefaultKind =>
        Person.Default(
          p.id,
          p.lastname,
          p.firstname,
          p.title,
          faculties,
          p.abbreviation,
          p.campusId.get,
          p.status
        )
      case Person.GroupKind =>
        Person.Group(p.id, p.title)
      case Person.UnknownKind =>
        Person.Unknown(p.id, p.title)
    }

  def toDbEntry(p: Person): PersonDbEntry =
    p match {
      case Default(
            id,
            lastname,
            firstname,
            title,
            faculties,
            abbreviation,
            campusId,
            status
          ) =>
        PersonDbEntry(
          id,
          lastname,
          firstname,
          title,
          faculties.map(_.abbrev),
          abbreviation,
          Some(campusId),
          status,
          Person.DefaultKind
        )
      case Group(id, title) =>
        PersonDbEntry(
          id,
          "",
          "",
          title,
          Nil,
          "",
          None,
          PersonStatus.Active,
          Person.GroupKind
        )
      case Unknown(id, title) =>
        PersonDbEntry(
          id,
          "",
          "",
          title,
          Nil,
          "",
          None,
          PersonStatus.Active,
          Person.UnknownKind
        )
    }
}

sealed trait PersonStatus {
  def deLabel: String
  def enLabel: String

  override def toString = enLabel
}

object PersonStatus {
  case object Active extends PersonStatus {
    override def deLabel = "aktiv"
    override def enLabel = "active"
  }

  case object Inactive extends PersonStatus {
    override def deLabel = "inaktiv"
    override def enLabel = "inactive"
  }

  case object Unknown extends PersonStatus {
    override def deLabel = "unbekannt"
    override def enLabel = "unknown"
  }

  def apply(string: String): PersonStatus =
    string.toLowerCase match {
      case "active"   => Active
      case "inactive" => Inactive
      case _          => Unknown
    }
}
