package models

import play.api.libs.json.*

enum EmploymentType(val id: String) {
  case Professor       extends EmploymentType("prof")
  case WMA             extends EmploymentType("wma")
  case AdjunctLecturer extends EmploymentType("adjunct_lecturer")
  case Unknown         extends EmploymentType("unknown")
}

object EmploymentType {
  given Format[EmploymentType] = Format(
    Reads
      .of[String]
      .flatMapResult(id =>
        values.find(_.id == id).fold[JsResult[EmploymentType]](JsError("Unknown EmploymentType: " + id))(JsSuccess(_))
      ),
    Writes(value => JsString(value.id))
  )

  def apply(id: String) =
    id match {
      case "prof"             => Professor
      case "wma"              => WMA
      case "adjunct_lecturer" => AdjunctLecturer
      case _                  => Unknown
    }
}
