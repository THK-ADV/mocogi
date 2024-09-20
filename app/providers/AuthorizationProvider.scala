package providers

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import auth._
import org.keycloak.adapters.KeycloakDeploymentBuilder
import play.api.Environment

@Singleton
final class AuthorizationProvider @Inject() (env: Environment) extends Provider[Authorization[UserToken]] {

  private def tokenFactory(): TokenFactory[UserToken] =
    (attributes: Map[String, AnyRef], mail: String, roles: Set[String]) => {
      def get(attr: String) =
        if (attributes.contains(attr)) Right(attributes(attr).toString)
        else Left(s"user attribute '$attr' not found")
      for {
        firstname <- get("firstname")
        lastname  <- get("lastname")
        username  <- get("username")
      } yield UserToken(firstname, lastname, username, mail, roles)
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
