package auth

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api.libs.json.Json
import play.api.mvc.*
import play.api.mvc.Results.Unauthorized

@Singleton
case class AuthorizationAction @Inject() (
    auth: Authorization,
    parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[TokenRequest, AnyContent] {

  override def invokeBlock[A](
      request: Request[A],
      block: TokenRequest[A] => Future[Result]
  ): Future[Result] =
    auth
      .authorize(request.headers.get(Authorization.AuthorizationHeader))
      .flatMap(token => block(TokenRequest(request, token)))
      .recover {
        case e: Throwable =>
          Unauthorized(
            Json.obj(
              "request" -> request.toString(),
              "message" -> e.getMessage
            )
          )
      }
}
