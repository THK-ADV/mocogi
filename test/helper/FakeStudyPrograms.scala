package helper

import basedata.StudyProgramWithPO

trait FakeStudyPrograms {
  implicit def fakeStudyPrograms: Seq[StudyProgramWithPO] = Seq(
    StudyProgramWithPO("ai2"),
    StudyProgramWithPO("mi4"),
    StudyProgramWithPO("wi5"),
    StudyProgramWithPO("itm2"),
  )
}
