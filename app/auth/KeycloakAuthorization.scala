package auth
import scala.jdk.CollectionConverters.MapHasAsScala
import scala.jdk.CollectionConverters.SetHasAsScala
import scala.util.Try

import auth.Authorization.AuthorizationHeader
import auth.Authorization.BearerPrefix
import org.keycloak.adapters.rotation.AdapterTokenVerifier
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.representations.AccessToken

final class KeycloakAuthorization[Token](
    keycloakDeployment: KeycloakDeployment,
    tokenFactory: TokenFactory[Token]
) extends Authorization[Token] {

  override def authorize(
      authorizationHeaderValue: Option[String]
  ): Try[Token] =
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
      .getOrElse(throw new Exception(s"could not find $BearerPrefix Token in $AuthorizationHeader header"))

  private def extractAttributes(accessToken: AccessToken): Token = {
    val attributes = accessToken.getOtherClaims.asScala.toMap
    val mail       = Option(accessToken.getEmail)
    val roles      = accessToken.getRealmAccess.getRoles.asScala.toSet
    tokenFactory.create(attributes, mail, roles) match
      case Right(token) => token
      case Left(err)    => throw new Exception(s"Failed to create Token: $err")
  }

  private def verifyToken(token: String): AccessToken =
    AdapterTokenVerifier
      .createVerifier(token, keycloakDeployment, true, classOf[AccessToken])
      .verify()
      .getToken
}
