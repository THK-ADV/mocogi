import play.api.mvc.{AnyContent, Request}
import printing.PrintingLanguage

package object controllers {
  implicit class RequestOps(private val self: Request[AnyContent])
      extends AnyVal {
    def parseLang(): PrintingLanguage =
      self
        .getQueryString("lang")
        .flatMap(PrintingLanguage.apply)
        .getOrElse(PrintingLanguage.German)
  }

  object MimeTypes {
    val JSON = "application/json"

    val PDF = "application/pdf"
  }
}
