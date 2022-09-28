package parsing.types

case class POMandatory(
    studyProgram: StudyProgram,
    recommendedSemester: List[Int],
    recommendedSemesterPartTime: List[Int]
)
