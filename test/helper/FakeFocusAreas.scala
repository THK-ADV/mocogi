package helper

import basedata.FocusAreaPreview

trait FakeFocusAreas {
  implicit def fakeFocusAreas: Seq[FocusAreaPreview] =
    Seq(
      FocusAreaPreview("gak"),
      FocusAreaPreview("acs")
    )
}
