package helper

import parsing.types.FocusArea

trait FakeFocusAreas {
  implicit def fakeFocusAreas: Seq[FocusArea] =
    Seq(
      FocusArea("gak"),
      FocusArea("acs")
    )
}
