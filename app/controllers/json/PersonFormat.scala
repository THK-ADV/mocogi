package controllers.json

import basedata.Person
import play.api.libs.json.{Format, Json}

trait PersonFormat extends FacultyFormat {
  implicit val personFormat: Format[Person] =
    Json.format[Person]
}
