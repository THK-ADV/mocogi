package helper

import parsing.types.Person

trait FakePersons {
  implicit def fakePersons: Seq[Person] = Seq(
    Person("ald", "Dobrynin", "Alexander", "M.Sc.", "F10"),
    Person("abe", "Bertels", "Anja", "B.Sc.", "F10"),
    Person("ddu", "Dubbert", "Dennis", "M.Sc.", "F10"),
  )
}
