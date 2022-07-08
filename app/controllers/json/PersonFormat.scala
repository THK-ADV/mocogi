package controllers.json

import parsing.types.Person
import play.api.libs.json.{Format, Json}

trait PersonFormat {
  implicit val personFormat: Format[Person] =
    Json.format[Person]
}
