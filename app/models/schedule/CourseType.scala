package models.schedule

enum CourseType(val id: String) {
  case Lecture extends CourseType("lecture")
  case Lab extends CourseType("lab")
  case Exercise extends CourseType("exercise")
  case Seminar extends CourseType("seminar")
  case Tutorial extends CourseType("tutorial")
}
