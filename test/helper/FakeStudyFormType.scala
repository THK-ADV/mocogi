package helper

import models.core.StudyFormType

trait FakeStudyFormType {
  implicit def fakeStudyFormType: Seq[StudyFormType] = Seq(
    StudyFormType("full", "", ""),
    StudyFormType("part", "", ""),
    StudyFormType("dual", "", ""),
    StudyFormType("bbw", "", ""),
    StudyFormType("bbs", "", "")
  )
}
