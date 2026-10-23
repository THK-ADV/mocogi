package auth

import ops.ConfigurationOps.nonEmptyString
import play.api.Configuration

final case class KeycloakConfig(jwksUrl: String, issuer: String)

object KeycloakConfig {
  def load(configuration: Configuration): KeycloakConfig =
    KeycloakConfig(
      jwksUrl = configuration.nonEmptyString("keycloak.jwksUrl"),
      issuer = configuration.nonEmptyString("keycloak.issuer")
    )
}
