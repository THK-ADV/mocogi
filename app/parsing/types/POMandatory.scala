package parsing.types

case class POMandatory(
    studyProgram: String,
    recommendedSemester: List[Int],
    recommendedSemesterPartTime: List[Int]
)
