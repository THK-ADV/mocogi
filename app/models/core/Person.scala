package models.core

sealed trait Person {
  def id: String
  def kind: String
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
  }

  case class Group(id: String, label: String) extends Person {
    override val kind = GroupKind
  }

  case class Unknown(id: String, label: String) extends Person {
    override val kind = UnknownKind
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
