package helper

import basedata.StudyProgramWithPO

trait FakeStudyProgramsWithPO {
  implicit def fakeStudyProgramsWithPo: Seq[StudyProgramWithPO] = Seq(
    StudyProgramWithPO("ai2"),
    StudyProgramWithPO("mi4"),
    StudyProgramWithPO("wi5"),
    StudyProgramWithPO("itm2"),
  )
}
