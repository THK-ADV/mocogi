package helper

import basedata.StudyProgramPreview

trait FakeStudyProgramPreviews {
  lazy val inf = StudyProgramPreview("inf_inf")

  lazy val itm = StudyProgramPreview("inf_itm")

  lazy val mi = StudyProgramPreview("inf_mi")

  lazy val wi = StudyProgramPreview("inf_wi")

  implicit def fakeStudyProgramPreviews: Seq[StudyProgramPreview] =
    Seq(inf, itm, mi, wi)
}
