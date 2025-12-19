package auth

import scala.concurrent.Future

import com.google.inject.ImplementedBy

@ImplementedBy(classOf[JwtAuthorization])
trait Authorization {
  def authorize(headerValue: Option[String]): Future[Token]
}

object Authorization {
  val AuthorizationHeader = "Authorization"
  val BearerPrefix        = "Bearer"
}
