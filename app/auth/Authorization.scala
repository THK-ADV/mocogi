package auth

import scala.util.Try

trait Authorization[UserToken] {
  def authorize(authorizationHeaderValue: Option[String]): Try[UserToken]
}

object Authorization {
  val AuthorizationHeader = "Authorization"
  val BearerPrefix        = "Bearer"
}
