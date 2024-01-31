import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.{
  Json,
  JsonConfiguration,
  OptionHandlers,
  Reads,
  Writes
}
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

  // TODO test if this is implicitly applied to all json types
  implicit val config: Aux[Json.MacroOptions] =
    JsonConfiguration(optionHandlers = OptionHandlers.WritesNull)

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
