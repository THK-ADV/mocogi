package models

import play.api.libs.json.*

enum AssessmentMethodSource(val id: String) {
  case Unknown extends AssessmentMethodSource("unknown")
  case RPO     extends AssessmentMethodSource("rpo")
}

object AssessmentMethodSource {
  given Format[AssessmentMethodSource] = Format(
    Reads
      .of[String]
      .flatMapResult(id =>
        values
          .find(_.id == id)
          .fold[JsResult[AssessmentMethodSource]](JsError("Unknown AssessmentMethodSource: " + id))(JsSuccess(_))
      ),
    Writes(value => JsString(value.id))
  )

  def apply(id: String): AssessmentMethodSource =
    id match {
      case "unknown" => Unknown
      case "rpo"     => RPO
      case other     => throw new IllegalArgumentException(s"Unknown AssessmentMethodSource: $other")
    }
}
