package helper

import basedata.Status

trait FakeStatus {
  implicit def fakeStatus: Seq[Status] = Seq(
    Status("active", "Aktiv", "--"),
    Status("inactive", "Inaktiv", "--")
  )
}
