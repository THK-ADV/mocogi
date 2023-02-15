package ops

import play.api.libs.json.{JsError, JsResult, JsSuccess}

import scala.util.{Failure, Success, Try}

object JsResultOps {
  implicit class Ops[A](jsResult: JsResult[A]) {
    def toTry: Try[A] = jsResult match {
      case JsSuccess(value, _) => Success(value)
      case JsError(errors)     => Failure(new Throwable(errors.mkString("\n")))
    }
  }
}
