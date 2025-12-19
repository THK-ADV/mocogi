package auth

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import play.api.libs.json.Json
import play.api.mvc.*
import play.api.mvc.Results.Unauthorized

@Singleton
case class AuthorizationAction @Inject() (
    auth: Authorization[Token],
    parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[TokenRequest, AnyContent] {

  override def invokeBlock[A](
      request: Request[A],
      block: TokenRequest[A] => Future[Result]
  ): Future[Result] = {
    auth.authorize(request.headers.get(Authorization.AuthorizationHeader)) match {
      case Success(token) => block(TokenRequest(request, token))
      case Failure(e)     =>
        Future.successful(
          Unauthorized(
            Json.obj(
              "request" -> request.toString(),
              "message" -> e.getMessage
            )
          )
        )
    }
  }
}
