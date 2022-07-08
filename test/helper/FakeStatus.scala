package helper

import parsing.types.Status

trait FakeStatus {
  implicit def fakeStatus: Seq[Status] = Seq(
    Status("active", "Aktiv", "--"),
    Status("inactive", "Inaktiv", "--")
  )
}
