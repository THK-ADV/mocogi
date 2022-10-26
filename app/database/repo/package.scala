package database

import basedata.{Faculty, Person}
import database.entities.PersonDbEntry

package object repo {
  def makePerson(p: PersonDbEntry, faculties: List[Faculty]): Person = {
    p.kind match {
      case Person.SingleKind =>
        Person.Single(
          p.id,
          p.lastname,
          p.firstname,
          p.title,
          faculties,
          p.abbreviation,
          p.status
        )
      case Person.GroupKind =>
        Person.Group(p.id, p.title)
      case Person.UnknownKind =>
        Person.Unknown(p.id, p.title)
    }
  }
}
