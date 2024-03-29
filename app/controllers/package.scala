import play.api.libs.json.{Json, Reads, Writes}
import play.api.mvc.{AnyContent, Request}
import printing.PrintingLanguage

package object controllers {

  implicit def listReads[A](implicit reads: Reads[A]): Reads[List[A]] =
    Reads.list(reads)

  implicit def throwableWrites: Writes[Throwable] = t =>
    Json.obj(
      "type" -> "throwable",
      "message" -> t.getMessage
    )

  implicit class RequestOps(private val self: Request[AnyContent])
      extends AnyVal {
    def parseLang(): PrintingLanguage =
      self
        .getQueryString("lang")
        .flatMap(PrintingLanguage.apply)
        .getOrElse(PrintingLanguage.German)

    def isExtended: Boolean =
      self
        .getQueryString("extend")
        .flatMap(_.toBooleanOption)
        .getOrElse(false)
  }

  object MimeTypes {
    val JSON = "application/json"

    val PDF = "application/pdf"
  }
}
