package helper

import models.core.Status

trait FakeStatus {
  implicit def fakeStatus: Seq[Status] = Seq(
    Status("active", "Aktiv", "--"),
    Status("inactive", "Inaktiv", "--")
  )
}
