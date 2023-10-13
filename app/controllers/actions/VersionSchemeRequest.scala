package controllers.actions

import auth.UserTokenRequest
import parsing.metadata.VersionScheme
import play.api.mvc.WrappedRequest

case class VersionSchemeRequest[A](
    versionScheme: VersionScheme,
    request: UserTokenRequest[A]
) extends WrappedRequest[A](request)
