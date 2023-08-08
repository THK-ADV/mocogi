package controllers.formats

import models.core.{Person, PersonStatus}
import play.api.libs.json._

trait PersonFormat extends FacultyFormat {
  private implicit val unknownPersonReads: Reads[Person.Unknown] =
    Json.reads[Person.Unknown]

  private implicit val unknownPersonWrites: Writes[Person.Unknown] =
    Json
      .writes[Person.Unknown]
      .transform((js: JsObject) =>
        js + ("kind" -> JsString(Person.UnknownKind))
      )

  implicit lazy val singlePersonReads: Reads[Person.Default] =
    Json.reads[Person.Default]

  implicit lazy val singlePersonWrites: Writes[Person.Default] =
    Json
      .writes[Person.Default]
      .transform((js: JsObject) => js + ("kind" -> JsString(Person.DefaultKind)))

  implicit val groupPersonReads: Reads[Person.Group] =
    Json.reads[Person.Group]

  implicit val groupPersonWrites: Writes[Person.Group] =
    Json
      .writes[Person.Group]
      .transform((js: JsObject) => js + ("kind" -> JsString(Person.GroupKind)))

  implicit val personStatusFmt: Format[PersonStatus] =
    Format.of[String].bimap(PersonStatus.apply, _.toString)

  implicit val personFormat: Format[Person] =
    Format.apply(
      js =>
        js.\("kind")
          .validate[String]
          .flatMap {
            case Person.DefaultKind  => singlePersonReads.reads(js)
            case Person.GroupKind   => groupPersonReads.reads(js)
            case Person.UnknownKind => unknownPersonReads.reads(js)
            case other =>
              JsError(
                s"kind must be ${Person.DefaultKind}, ${Person.GroupKind} or ${Person.UnknownKind}, but was $other"
              )
          },
      {
        case single: Person.Default =>
          singlePersonWrites.writes(single)
        case group: Person.Group =>
          groupPersonWrites.writes(group)
        case unknown: Person.Unknown =>
          unknownPersonWrites.writes(unknown)
      }
    )
}
