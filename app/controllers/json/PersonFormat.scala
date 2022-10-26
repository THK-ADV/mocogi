package controllers.json

import basedata.{Person, PersonStatus}
import play.api.libs.json.{Format, JsError, Json}

trait PersonFormat extends FacultyFormat {
  implicit val unknownPersonFmt: Format[Person.Unknown] =
    Json.format[Person.Unknown]

  implicit lazy val singlePersonFmt: Format[Person.Single] =
    Json.format[Person.Single]

  implicit val groupPersonFmt: Format[Person.Group] =
    Json.format[Person.Group]

  implicit val personStatusFmt: Format[PersonStatus] =
    Format.of[String].bimap(PersonStatus.apply, _.toString)

  implicit val personFormat: Format[Person] =
    Format.apply(
      js =>
        js.\("kind")
          .validate[String]
          .flatMap {
            case Person.SingleKind  => singlePersonFmt.reads(js)
            case Person.GroupKind   => groupPersonFmt.reads(js)
            case Person.UnknownKind => unknownPersonFmt.reads(js)
            case other =>
              JsError(
                s"kind must be ${Person.SingleKind}, ${Person.GroupKind} or ${Person.UnknownKind}, but was $other"
              )
          },
      {
        case single: Person.Single =>
          singlePersonFmt.writes(single)
        case group: Person.Group =>
          groupPersonFmt.writes(group)
        case unknown: Person.Unknown =>
          unknownPersonFmt.writes(unknown)
      }
    )
}
