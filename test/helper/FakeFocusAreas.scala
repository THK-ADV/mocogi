package helper

import models.core.FocusAreaID

trait FakeFocusAreas {
  implicit def fakeFocusAreas: Seq[FocusAreaID] =
    Seq(
      FocusAreaID("gak"),
      FocusAreaID("acs"),
      FocusAreaID("bs"),
    )
}
