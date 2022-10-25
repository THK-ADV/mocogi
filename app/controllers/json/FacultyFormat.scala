package controllers.json

import basedata.Faculty
import play.api.libs.json.{Format, Json}

trait FacultyFormat {
  implicit val facultyFormat: Format[Faculty] =
    Json.format[Faculty]
}
