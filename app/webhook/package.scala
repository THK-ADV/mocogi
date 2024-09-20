import scala.collection.Seq

import play.api.libs.json.JsPath
import play.api.libs.json.JsValue
import play.api.libs.json.JsonValidationError
import play.api.Logger

package object webhook {
  case class HandleEvent(json: JsValue) extends AnyVal

  def mkString[A](xs: Seq[A]): String =
    xs.mkString("\n\t- ", "\n\t- ", "")

  def logUnhandedEvent(
      logger: Logger,
      errors: Seq[(JsPath, Seq[JsonValidationError])]
  ): Unit = {
    logger.info(
      s"unable to handle event. errors: ${mkString(errors.map(_._2.mkString(", ")).toSeq)}"
    )
  }
}
