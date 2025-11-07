package controllers.actions

import parsing.metadata.VersionScheme
import play.api.mvc.WrappedRequest

case class VersionSchemeRequest[A](
    versionScheme: VersionScheme,
    request: PersonRequest[A]
) extends WrappedRequest[A](request)
