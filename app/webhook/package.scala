import scala.collection.Seq

import git.Branch
import play.api.libs.json.JsPath
import play.api.libs.json.JsResult
import play.api.libs.json.JsValue
import play.api.libs.json.JsonValidationError
import play.api.Logger

package object webhook {
  case class HandleEvent(json: JsValue) extends AnyVal

  def mkString[A](xs: Seq[A]): String =
    xs.mkString("\n\t- ", "\n\t- ", "")

  def parseBranch(json: JsValue): JsResult[Branch] =
    json
      .\("ref")
      .validate[String]
      .map(_.split("/").last)
      .map(Branch.apply)

  def logUnhandedEvent(
      logger: Logger,
      errors: Seq[(JsPath, Seq[JsonValidationError])]
  ): Unit = {
    logger.info(
      s"unable to handle event. errors: ${mkString(errors.map(_._2.mkString(", ")).toSeq)}"
    )
  }
}
