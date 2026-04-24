package controllers

import play.api.mvc.RequestHeader
import play.api.mvc.Result
import security.ClientErrorResponse

trait UsesClientErrors {
  protected def clientErrors: ClientErrorResponse

  protected def forbiddenUserMessage(username: String, detail: Option[String]): String =
    detail.map(_.trim) match {
      case Some(detail) if detail.nonEmpty => s"user $username has insufficient permissions $detail"
      case _                               => s"user $username has insufficient permissions to perform this action"
    }

  protected def forbiddenForUser(request: RequestHeader, username: String, detail: Option[String] = None): Result =
    clientErrors.forbidden(request, forbiddenUserMessage(username, detail))
}
