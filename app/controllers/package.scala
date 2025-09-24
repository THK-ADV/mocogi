import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc.RequestHeader

package object controllers {

  implicit def listReads[A](implicit reads: Reads[A]): Reads[List[A]] =
    Reads.list(reads)

  given Writes[Throwable] = t =>
    Json.obj(
      "type"    -> "exception",
      "message" -> t.getMessage
    )

  given Writes[Exception] = e =>
    Json.obj(
      "type"    -> "exception",
      "message" -> e.getMessage
    )

  extension (self: RequestHeader) {
    def isNewApi: Boolean =
      self
        .getQueryString("newApi")
        .flatMap(_.toBooleanOption)
        .getOrElse(false)

    def isExtended: Boolean =
      self
        .getQueryString("extend")
        .flatMap(_.toBooleanOption)
        .getOrElse(false)
  }

  object MimeTypes {
    val JSON = "application/json"
    val PDF  = "application/pdf"
  }
}
