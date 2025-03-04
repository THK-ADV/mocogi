package helper

import models.core.Identity
import models.EmploymentType.WMA

trait FakeIdentities {
  implicit def fakeIdentities: Seq[Identity] = Seq(
    Identity.Person(
      "ald",
      "Dobrynin",
      "Alexander",
      "M.Sc.",
      List("f10"),
      "ad",
      Some("ald"),
      isActive = true,
      WMA,
      None,
      None
    ),
    Identity.Person(
      "abe",
      "Bertels",
      "Anja",
      "B.Sc.",
      List("f10"),
      "ab",
      Some("abe"),
      isActive = true,
      WMA,
      None,
      None
    ),
    Identity.Person(
      "ddu",
      "Dubbert",
      "Dennis",
      "M.Sc.",
      List("f10"),
      "dd",
      None,
      isActive = true,
      WMA,
      None,
      None
    ),
    unknown
  )

  def unknown = Identity.NN
}
