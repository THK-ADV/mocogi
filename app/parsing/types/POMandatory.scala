package parsing.types

import basedata.StudyProgram

case class POMandatory(
    studyProgram: StudyProgram,
    recommendedSemester: List[Int],
    recommendedSemesterPartTime: List[Int]
)
