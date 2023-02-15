package helper

import models.core.{Person, PersonStatus}

trait FakePersons extends FakeFaculties {
  implicit def fakePersons: Seq[Person] = Seq(
    Person.Single("ald", "Dobrynin", "Alexander", "M.Sc.", List(f10), "ad", PersonStatus.Active),
    Person.Single("abe", "Bertels", "Anja", "B.Sc.", List(f10), "ab", PersonStatus.Active),
    Person.Single("ddu", "Dubbert", "Dennis", "M.Sc.", List(f10), "dd", PersonStatus.Active),
    Person.Unknown("nn", "N.N")
  )
}
