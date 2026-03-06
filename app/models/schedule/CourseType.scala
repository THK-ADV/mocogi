package models.schedule

import play.api.libs.json.Format
import play.api.libs.json.Reads

enum CourseType(val id: String) {
  case Lecture  extends CourseType("lecture")
  case Lab      extends CourseType("lab")
  case Exercise extends CourseType("exercise")
  case Seminar  extends CourseType("seminar")
  case Tutorial extends CourseType("tutorial")
}

object CourseType {
  given Format[CourseType] = Format.of[String].bimap(apply, _.id)

  def apply(id: String) =
    id match {
      case "lecture"  => Lecture
      case "lab"      => Lab
      case "exercise" => Exercise
      case "seminar"  => Seminar
      case "tutorial" => Tutorial
    }
}
