package parsing.types

import basedata.StudyProgram

case class POs(mandatory: List[POMandatory], optional: List[POOptional])

case class POOptional(
  studyProgram: StudyProgram,
  instanceOf: String,
  partOfCatalog: Boolean,
  recommendedSemester: List[Int]
)

case class POMandatory(
  studyProgram: StudyProgram,
  recommendedSemester: List[Int],
  recommendedSemesterPartTime: List[Int]
)
