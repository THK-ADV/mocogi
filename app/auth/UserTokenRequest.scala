package auth

import play.api.mvc.{Request, WrappedRequest}

case class UserTokenRequest[A](unwrapped: Request[A], token: UserToken)
    extends WrappedRequest[A](unwrapped) {
  def campusId = CampusId(token.username)
}
