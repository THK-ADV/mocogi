package auth

import play.api.mvc.Request
import play.api.mvc.WrappedRequest

case class UserTokenRequest[A](unwrapped: Request[A], token: UserToken) extends WrappedRequest[A](unwrapped) {
  def campusId = CampusId(token.username)
}
