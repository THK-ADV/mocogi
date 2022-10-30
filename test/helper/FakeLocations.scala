package helper

import basedata.Location

trait FakeLocations {
  implicit def fakeLocations: Seq[Location] = Seq(
    Location("gm", "Gummersbach", "--"),
    Location("dz", "Deutz", "--"),
    Location("su", "Südstadt", "--"),
    Location("km", "Köln-Mühlheim", "--"),
  )
}
