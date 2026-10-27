package auth

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import play.api.mvc.*
import security.ClientErrorResponse

@Singleton
case class AuthorizationAction @Inject() (
    auth: Authorization,
    parser: BodyParsers.Default,
    clientErrors: ClientErrorResponse
)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[TokenRequest, AnyContent] {

  override def invokeBlock[A](
      request: Request[A],
      block: TokenRequest[A] => Future[Result]
  ): Future[Result] =
    auth
      .authorize(request.headers.get(Authorization.AuthorizationHeader))
      .transformWith {
        case Success(token) => block(TokenRequest(request, token))
        case Failure(error) => Future.successful(clientErrors.unauthorized(request, error))
      }
}
