import play.api.i18n.Lang
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc.RequestHeader
import printing.PrintingLanguage

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

  implicit class LangOps(private val self: Lang) extends AnyVal {
    def toPrintingLang(): PrintingLanguage =
      if (self.code.startsWith("de")) PrintingLanguage.German
      else PrintingLanguage.English
  }

  extension (self: RequestHeader) {
    def isNewApi: Boolean =
      self
        .getQueryString("newApi")
        .flatMap(_.toBooleanOption)
        .getOrElse(false)

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
    val PDF  = "application/pdf"
  }
}
