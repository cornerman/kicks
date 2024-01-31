package kicks.http.authn

import cats.effect.IO
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.{JWT, JWTVerifier}
import com.auth0.jwt.algorithms.Algorithm

import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

private class TokenVerifier(jwkUrl: String, audiences: Set[String], keychainTTLMinutes: Int) {
  private val provider =
    new JwkProviderBuilder(jwkUrl).cached(10, keychainTTLMinutes, TimeUnit.MINUTES).rateLimited(10, 1, TimeUnit.MINUTES).build()

  def verify(token: String): IO[String] = for {
    decodedJWT  <- IO(JWT.decode(token))
    jwk         <- IO.blocking(provider.get(decodedJWT.getKeyId))
    algorithm    = Algorithm.RSA256(jwk.getPublicKey.asInstanceOf[RSAPublicKey], null)
    verifier     = JWT.require(algorithm).withAudience(audiences.toSeq: _*).build()
    verifiedJWT <- IO(verifier.verify(decodedJWT))
  } yield verifiedJWT.getPayload
}
