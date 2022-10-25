import basedata.Person
import database.entities.PersonDbEntry

package object service {
  def makePersonDbEntry(p: Person): PersonDbEntry =
    PersonDbEntry(p.abbrev, p.lastname, p.firstname, p.title, p.faculty.abbrev)
}
