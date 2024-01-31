package helper

import models.core.{Identity, PersonStatus}

trait FakeIdentities extends FakeFaculties {
  implicit def fakeIdentities: Seq[Identity] = Seq(
    Identity.Person(
      "ald",
      "Dobrynin",
      "Alexander",
      "M.Sc.",
      List(f10),
      "ad",
      "ald",
      PersonStatus.Active
    ),
    Identity.Person(
      "abe",
      "Bertels",
      "Anja",
      "B.Sc.",
      List(f10),
      "ab",
      "abe",
      PersonStatus.Active
    ),
    Identity.Person(
      "ddu",
      "Dubbert",
      "Dennis",
      "M.Sc.",
      List(f10),
      "dd",
      "ddu",
      PersonStatus.Active
    ),
    Identity.Unknown("nn", "N.N")
  )
}
