package auth

import play.api.mvc.Request
import play.api.mvc.WrappedRequest

case class TokenRequest[A](unwrapped: Request[A], token: Token) extends WrappedRequest[A](unwrapped) {
  def campusId = CampusId(token.username) // this does not work with Token.ServiceToken
}
