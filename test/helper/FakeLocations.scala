package helper

import parsing.types.Location

trait FakeLocations {
  implicit def fakeLocations: Seq[Location] = Seq(
    Location("gm", "Gummersbach", "--"),
    Location("dz", "Deutz", "--")
  )
}
