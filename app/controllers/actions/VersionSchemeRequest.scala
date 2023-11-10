package controllers.actions

import controllers.actions.PersonAction.PersonRequest
import parsing.metadata.VersionScheme
import play.api.mvc.WrappedRequest

case class VersionSchemeRequest[A](
    versionScheme: VersionScheme,
    request: PersonRequest[A]
) extends WrappedRequest[A](request)
