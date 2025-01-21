package auth

import scala.util.Try

trait Authorization[Token] {
  def authorize(authorizationHeaderValue: Option[String]): Try[Token]
}

object Authorization {
  val AuthorizationHeader = "Authorization"
  val BearerPrefix        = "Bearer"
}
