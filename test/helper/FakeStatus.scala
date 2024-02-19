package helper

import models.core.ModuleStatus

trait FakeStatus {
  implicit def fakeStatus: Seq[ModuleStatus] = Seq(
    ModuleStatus("active", "Aktiv", "--"),
    ModuleStatus("inactive", "Inaktiv", "--")
  )
}
