package helper

import basedata.StudyProgram

trait FakeStudyPrograms {
  implicit def fakeStudyPrograms: Seq[StudyProgram] = Seq(
    StudyProgram("ai2"),
    StudyProgram("mi4"),
    StudyProgram("wi5"),
    StudyProgram("itm2"),
  )
}
