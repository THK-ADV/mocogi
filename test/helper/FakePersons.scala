package helper

import models.core.{Person, PersonStatus}

trait FakePersons extends FakeFaculties {
  implicit def fakePersons: Seq[Person] = Seq(
    Person.Default("ald", "Dobrynin", "Alexander", "M.Sc.", List(f10), "ad", "ald", PersonStatus.Active),
    Person.Default("abe", "Bertels", "Anja", "B.Sc.", List(f10), "ab", "abe", PersonStatus.Active),
    Person.Default("ddu", "Dubbert", "Dennis", "M.Sc.", List(f10), "dd", "ddu", PersonStatus.Active),
    Person.Unknown("nn", "N.N")
  )
}
