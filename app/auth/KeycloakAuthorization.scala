package auth
import auth.Authorization.{AuthorizationHeader, BearerPrefix}
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.rotation.AdapterTokenVerifier
import org.keycloak.representations.AccessToken

import scala.jdk.CollectionConverters.{MapHasAsScala, SetHasAsScala}
import scala.util.Try

final class KeycloakAuthorization[UserToken](
    keycloakDeployment: KeycloakDeployment,
    tokenFactory: TokenFactory[UserToken]
) extends Authorization[UserToken] {

  override def authorize(
      authorizationHeaderValue: Option[String]
  ): Try[UserToken] =
    Try {
      val bearerToken = extractBearerToken(authorizationHeaderValue)
      val accessToken = verifyToken(bearerToken)
      extractAttributes(accessToken)
    }

  private def extractBearerToken(
      authorizationHeaderValue: Option[String]
  ): String =
    authorizationHeaderValue
      .map(_.split(" "))
      .filter(_.length == 2)
      .filter(_.head.equalsIgnoreCase(BearerPrefix))
      .map(_.last)
      .filter(_.nonEmpty)
      .getOrElse(
        throw new Throwable(
          s"could not find $BearerPrefix Token in $AuthorizationHeader header"
        )
      )

  private def extractAttributes(accessToken: AccessToken): UserToken = {
    val attributes = accessToken.getOtherClaims.asScala.toMap
    val mail = accessToken.getEmail
    val roles = accessToken.getRealmAccess.getRoles.asScala.toSet
    tokenFactory.create(attributes, mail, roles) match {
      case Right(token) => token
      case Left(err) =>
        throw new Throwable(s"Failed to extract attributes from token: $err")
    }
  }

  private def verifyToken(token: String): AccessToken =
    AdapterTokenVerifier
      .createVerifier(token, keycloakDeployment, true, classOf[AccessToken])
      .verify()
      .getToken
}
