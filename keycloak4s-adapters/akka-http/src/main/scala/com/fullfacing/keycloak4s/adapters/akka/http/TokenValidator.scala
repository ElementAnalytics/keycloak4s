package com.fullfacing.keycloak4s.adapters.akka.http

import java.time.Instant

import cats.data.EitherT
import cats.implicits._
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.{JWKSet, RSAKey}
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.SignedJWT.parse
import monix.eval.Task
import monix.execution.Scheduler
import org.json4s.Formats

import scala.util.Try

class TokenValidator(host: String, port: String, realm: String)(implicit scheduler: Scheduler) {
  implicit val formats: Formats = org.json4s.DefaultFormats

  /* A cache containing the public JWK set of the Keycloak server. Re-cacheable. **/
  private val cache = new JwksCache(host, port, realm)

  /* The cached key set, with thread-safe mutability controlled by the cache. **/
  private val keySet = cache.keySet

  /**
   * Checks if the token is not expired and is not being used before the nbf (if defined).
   */
  private def validateTime(exp: Instant, nbf: Instant): Either[Throwable, Unit] = {
    val now = Instant.now()

    val nbfCond = nbf == Instant.EPOCH || now.isAfter(nbf)
    val expCond = now.isBefore(exp)

    if (nbfCond && expCond) ().asRight
    else if (!nbfCond) Errors.NOT_YET_VALID.asLeft
    else Errors.EXPIRED.asLeft
  }

  /**
   * Checks the key set cache for valid keys, re-caches once (and only once) if invalid.
   */
  private def checkKeySet(): Task[Either[Throwable, JWKSet]] = keySet.flatMap {
    case Right(_) => keySet
    case Left(_)  => cache.reobtainKeys().map(_.left.map(_ => Errors.JWKS_SERVER_ERROR))
  }

  /**
   * Attempts to obtain the public key matching the key ID in the token header.
   * Re-caches the key set once (and only once) if the key was not found.
   */
  private def matchPublicKey(keyId: String, keys: JWKSet, reattempted: Boolean = false): Task[Either[Throwable, RSAKey]] = {
    Try(keys.getKeyByKeyId(keyId)).toEither match {
      case Left(_) if !reattempted  => cache.reobtainKeys().flatMap(_ => matchPublicKey(keyId, keys, reattempted = true))
      case Left(_)                  => Task(Errors.PUBLIC_KEY_NOT_FOUND.asLeft)
      case Right(k: RSAKey)         => Task(k.asRight)
    }
  }

  /**
   * Validates the token with the public key obtained from the Keycloak server.
   */
  private def validateSignature(token: SignedJWT, publicKey: RSAKey): Either[Throwable, Unit] = {
    val verifier = new RSASSAVerifier(publicKey)
    if (token.verify(verifier)) ().asRight else Errors.SIG_INVALID.asLeft
  }

  /**
   * Parses a bearer token, validate the token's expiration, nbf and signature, and returns the token payload.
   */
  def validate(rawToken: String): Task[Either[Throwable, Payload]] = {
    val token = parse(rawToken)
    val nbf = token.getJWTClaimsSet.getNotBeforeTime.toInstant
    val exp = token.getJWTClaimsSet.getExpirationTime.toInstant

    for {
      _     <- EitherT.fromEither[Task](validateTime(exp, nbf))
      keys  <- EitherT(checkKeySet())
      key   <- EitherT(matchPublicKey(token.getHeader.getKeyID, keys))
      _     <- EitherT.fromEither[Task](validateSignature(token, key))
    } yield token.getPayload
  }.value
}

object TokenValidator {
  def apply(host: String, port: String, realm: String)(implicit s: Scheduler) = new TokenValidator(host, port, realm)
}
