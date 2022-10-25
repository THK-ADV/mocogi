package database

import basedata.{Faculty, Person}
import database.entities.PersonDbEntry

package object repo {
  def makePerson(t: (PersonDbEntry, Faculty)): Person =
    Person(t._1.abbrev, t._1.lastname, t._1.firstname, t._1.title, t._2)
}
