package basedata

import play.api.libs.json.{Format, Json}

trait FacultyFormat {
  implicit val facultyFormat: Format[Faculty] =
    Json.format[Faculty]
}
