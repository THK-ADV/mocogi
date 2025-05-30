package providers

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import auth.*
import org.keycloak.adapters.KeycloakDeploymentBuilder
import play.api.Environment

@Singleton
final class AuthorizationProvider @Inject() (env: Environment) extends Provider[Authorization[Token]] {

  private def tokenFactory(): TokenFactory[Token] =
    (attributes: Map[String, AnyRef], mail: Option[String], roles: Set[String]) => {
      def get(attr: String) =
        if attributes.contains(attr) then Right(attributes(attr).toString)
        else Left(s"user attribute '$attr' not found")

      for
        username <- get("username")
        token <-
          if username.contains("service-account") then Right(Token.ServiceToken(username, roles))
          else
            for
              firstname <- get("firstname")
              lastname  <- get("lastname")
              mail      <- mail.toRight("mail attribute not found")
            yield Token.UserToken(firstname, lastname, username, mail, roles)
      yield token
    }

  private def keycloakDeployment() =
    KeycloakDeploymentBuilder.build(
      env.classLoader.getResourceAsStream("keycloak.json")
    )

  override def get() =
    new KeycloakAuthorization(
      keycloakDeployment(),
      tokenFactory()
    )
}
