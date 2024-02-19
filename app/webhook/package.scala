import play.api.Logger
import play.api.libs.json.{JsPath, JsValue, JsonValidationError}

import scala.collection.Seq

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
