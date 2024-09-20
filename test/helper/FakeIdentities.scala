package helper

import models.core.Identity
import models.core.PersonStatus

trait FakeIdentities extends FakeFaculties {
  implicit def fakeIdentities: Seq[Identity] = Seq(
    Identity.Person(
      "ald",
      "Dobrynin",
      "Alexander",
      "M.Sc.",
      List(f10.id),
      "ad",
      "ald",
      PersonStatus.Active
    ),
    Identity.Person(
      "abe",
      "Bertels",
      "Anja",
      "B.Sc.",
      List(f10.id),
      "ab",
      "abe",
      PersonStatus.Active
    ),
    Identity.Person(
      "ddu",
      "Dubbert",
      "Dennis",
      "M.Sc.",
      List(f10.id),
      "dd",
      "ddu",
      PersonStatus.Active
    ),
    unknown
  )

  def unknown = Identity.NN
}
