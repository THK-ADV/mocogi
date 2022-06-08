package parsing.types

case class Workload(
    total: Int,
    lecture: Int,
    seminar: Int,
    practical: Int,
    exercise: Int,
    selfStudy: Int
)
