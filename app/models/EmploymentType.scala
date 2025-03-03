package models

import play.api.libs.json.Format

enum EmploymentType(val id: String) {
  case Professor       extends EmploymentType("prof")
  case WMA             extends EmploymentType("wma")
  case AdjunctLecturer extends EmploymentType("adjunct_lecturer")
  case Unknown         extends EmploymentType("unknown")
}

object EmploymentType {
  given Format[EmploymentType] = Format.of[String].bimap(apply, _.id)

  def apply(id: String) =
    id match
      case "prof"             => Professor
      case "wma"              => WMA
      case "adjunct_lecturer" => AdjunctLecturer
      case _                  => Unknown
}
