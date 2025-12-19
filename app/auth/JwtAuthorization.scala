package auth

import java.math.BigInteger
import java.security.spec.RSAPublicKeySpec
import java.security.KeyFactory
import java.security.PublicKey
import java.util.Base64
import javax.inject.Inject

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import auth.Authorization.AuthorizationHeader
import auth.Authorization.BearerPrefix
import models.JsonParseException
import pdi.jwt.JwtAlgorithm
import pdi.jwt.JwtJson
import pdi.jwt.JwtOptions
import play.api.cache.AsyncCacheApi
import play.api.libs.json.*
import play.api.libs.ws.WSClient

/** JWT-based authorization that verifies tokens against Keycloak's JWKS endpoint. */
final class JwtAuthorization @Inject() (
    ws: WSClient,
    cache: AsyncCacheApi,
    config: KeycloakConfig
)(using ExecutionContext)
    extends Authorization {

  private val cacheKey = "keycloak-jwks"
  private val cacheTtl = 1.hour

  private def issuer  = config.issuer
  private def jwksUrl = config.jwksUrl

  /** Extracts and verifies a JWT from the Authorization header, returning the application token. */
  override def authorize(headerValue: Option[String]): Future[Token] =
    for
      bearerToken <- Future.fromTry(extractBearerToken(headerValue))
      claims      <- verifyAndDecode(bearerToken)
      token       <- Future.fromTry(extractToken(claims))
    yield token

  /** Parses the Authorization header and extracts the raw JWT string. */
  private def extractBearerToken(value: Option[String]): Try[String] =
    value
      .map(_.split(" "))
      .filter(_.length == 2)
      .filter(_.head.equalsIgnoreCase(BearerPrefix))
      .map(_.last)
      .filter(_.nonEmpty)
      .fold(Failure(new Exception(s"Expected $BearerPrefix Token in $AuthorizationHeader header")))(Success.apply)

  /** Verifies the JWT signature, validates the issuer, and returns the decoded claims. */
  private def verifyAndDecode(token: String): Future[JsObject] =
    for {
      (header, _, _) <- Future.fromTry(JwtJson.decodeAll(token, JwtOptions(signature = false)))
      kid            <- header.keyId.fold(Future.failed(new Exception("Expected kid in token")))(Future.successful)
      publicKey      <- getPublicKey(kid)
      claim          <- Future.fromTry(JwtJson.decode(token, publicKey, Seq(JwtAlgorithm.RS256)))
    } yield {
      val tokenIssuer = (Json.parse(claim.toJson) \ "iss").asOpt[String]
      if !tokenIssuer.contains(issuer) then throw Exception(s"Invalid issuer: expected $issuer, got $tokenIssuer")
      Json.parse(claim.content).as[JsObject]
    }

  /** Retrieves the RSA public key for the given key ID from the cached JWKS. */
  private def getPublicKey(kid: String): Future[PublicKey] =
    getJwks().map { jwks =>
      val key = (jwks \ "keys")
        .as[Seq[JsObject]]
        .find(k => (k \ "kid").asOpt[String].contains(kid))
        .getOrElse(throw Exception(s"Key with kid=$kid not found in JWKS"))

      val n = (key \ "n").as[String]
      val e = (key \ "e").as[String]

      buildRsaPublicKey(n, e)
    }

  /** Returns the cached JWKS or fetches it from Keycloak if not cached. */
  private def getJwks(): Future[JsValue] =
    cache.getOrElseUpdate(cacheKey, cacheTtl)(fetchJwks())

  /** Fetches the JWKS from Keycloak's well-known endpoint. */
  private def fetchJwks(): Future[JsValue] =
    ws.url(jwksUrl).get().map(_.json)

  /** Converts Base64URL-encoded RSA components into a Java PublicKey. */
  private def buildRsaPublicKey(n: String, e: String): PublicKey = {
    val decoder    = Base64.getUrlDecoder
    val modulus    = new BigInteger(1, decoder.decode(n))
    val exponent   = new BigInteger(1, decoder.decode(e))
    val spec       = new RSAPublicKeySpec(modulus, exponent)
    val keyFactory = KeyFactory.getInstance("RSA")
    keyFactory.generatePublic(spec)
  }

  /** Extracts user attributes, email, and roles from claims and creates the application token. */
  private def extractToken(claims: JsObject): Try[Token] = {
    val jsRes = for {
      roles    <- claims.\("realm_access").\("roles").validate[Set[String]]
      username <- claims.\("username").validate[String]
      token    <-
        if username.contains("service-account") then JsSuccess(Token.ServiceToken(username, roles))
        else
          for {
            firstname <- claims.\("firstname").validate[String]
            lastname  <- claims.\("lastname").validate[String]
            email     <- claims.\("email").validate[String]
          } yield Token.UserToken(firstname, lastname, username, email, roles)
    } yield token
    jsRes match {
      case JsSuccess(value, _) => Success(value)
      case JsError(errors)     => Failure(JsonParseException(errors))
    }
  }
}
