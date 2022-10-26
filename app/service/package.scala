import basedata.Person._
import basedata.{Person, PersonStatus}
import database.entities.PersonDbEntry

package object service {
  def makePersonDbEntry(p: Person): PersonDbEntry =
    p match {
      case Single(
            id,
            lastname,
            firstname,
            title,
            faculties,
            abbreviation,
            status
          ) =>
        PersonDbEntry(
          id,
          lastname,
          firstname,
          title,
          faculties.map(_.abbrev),
          abbreviation,
          status,
          Person.SingleKind
        )
      case Group(id, title) =>
        PersonDbEntry(
          id,
          "",
          "",
          title,
          Nil,
          "",
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
          PersonStatus.Active,
          Person.UnknownKind
        )
    }
}
