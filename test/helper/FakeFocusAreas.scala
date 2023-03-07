package helper

import models.core.FocusAreaPreview

trait FakeFocusAreas {
  implicit def fakeFocusAreas: Seq[FocusAreaPreview] =
    Seq(
      FocusAreaPreview("gak"),
      FocusAreaPreview("acs"),
      FocusAreaPreview("bs"),
    )
}
