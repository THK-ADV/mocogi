package helper

import models.core.ModuleLocation

trait FakeLocations {
  implicit def fakeLocations: Seq[ModuleLocation] = Seq(
    ModuleLocation("gm", "Gummersbach", "--"),
    ModuleLocation("dz", "Deutz", "--"),
    ModuleLocation("su", "Südstadt", "--"),
    ModuleLocation("km", "Köln-Mühlheim", "--"),
  )
}
