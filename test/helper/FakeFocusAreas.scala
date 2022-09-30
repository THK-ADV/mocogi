package helper

import basedata.FocusArea

trait FakeFocusAreas {
  implicit def fakeFocusAreas: Seq[FocusArea] =
    Seq(
      FocusArea("gak"),
      FocusArea("acs")
    )
}
