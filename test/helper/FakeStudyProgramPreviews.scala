package helper

import basedata.StudyProgramPreview

trait FakeStudyProgramPreviews {
  implicit def fakeStudyProgramPreviews: Seq[StudyProgramPreview] = Seq(
    StudyProgramPreview("inf_inf"),
    StudyProgramPreview("inf_itm"),
    StudyProgramPreview("inf_mi"),
    StudyProgramPreview("inf_wi"),
  )
}
