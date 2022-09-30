package validator

case class Workload(
    lecture: Int,
    seminar: Int,
    practical: Int,
    exercise: Int,
    projectSupervision: Int,
    projectWork: Int,
    selfStudy: Int,
    total: Int
)
