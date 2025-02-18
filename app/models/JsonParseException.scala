package models

import play.api.libs.json.JsPath
import play.api.libs.json.JsonValidationError

case class JsonParseException(errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])])
    extends Exception(
      errors
        .map {
          case (path, errors) =>
            s"$path: ${errors.map(_.message).mkString(", ")}"
        }
        .mkString("; ")
    )
