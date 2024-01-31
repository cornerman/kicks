package kicks.http.authn

import org.http4s._
import org.http4s.client.Client
import org.http4s.headers._
import cats.effect._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import kicks.http.http4sJsoniter.ArrayEntityCodec._

// Be compatible with usage in node:
// https: //github.com/keratin/authn-node/blob/master/src/Client.ts
case class AuthnClientConfig(
  issuer: String,
  audiences: Set[String], // authn-node allows String | String[]
  username: String,
  password: String,
  adminURL: Option[String] = None,
  keychainTTLMinutes: Int = 60,
)

class AuthnClient(config: AuthnClientConfig, httpClient: Client[IO]) {
  private val verifier = new TokenVerifier(config.issuer, config.audiences, config.keychainTTLMinutes)

  private def accountURL(id: Int, action: Option[String] = None): Uri =
    Uri.unsafeFromString(s"${config.adminURL.getOrElse(config.issuer)}/accounts/$id${action.fold("")("/" + _)}")

  private def authorizationHeaders: Headers = Headers(
    Authorization(BasicCredentials(config.username, config.password))
  )

  def account(id: Int): IO[Account] = {
    httpClient
      .expect[ServerResponse[Account]](
        Request[IO](
          Method.GET,
          accountURL(id),
          headers = authorizationHeaders,
        )
      )
      .map(_.result)
  }

  def updateAccount(id: Int, data: AccountUpdate): IO[Unit] = {
    httpClient.expect[Unit](
      Request[IO](
        method = Method.PATCH,
        uri = accountURL(id),
        headers = authorizationHeaders,
      ).withEntity(data)
    )
  }

  def archiveAccount(id: Int): IO[Unit] = {
    httpClient.expect[Unit](
      Request[IO](
        Method.DELETE,
        accountURL(id),
        headers = authorizationHeaders,
      )
    )
  }

  def lockAccount(id: Int): IO[Unit] = {
    httpClient.expect[Unit](
      Request[IO](
        Method.PATCH,
        accountURL(id, Some("lock")),
        headers = authorizationHeaders,
      )
    )
  }

  def unlockAccount(id: Int): IO[Unit] = {
    httpClient.expect[Unit](
      Request[IO](
        Method.PATCH,
        accountURL(id, Some("unlock")),
        headers = authorizationHeaders,
      )
    )
  }

  def importAccount(data: AccountImport): IO[AccountImported] = {
    httpClient
      .expect[ServerResponse[AccountImported]](
        Request[IO](
          Method.POST,
          Uri.fromString(s"${config.adminURL.getOrElse(config.issuer)}/accounts/import").toOption.get,
          headers = authorizationHeaders,
        ).withEntity(data)
      )
      .map(_.result)
  }

  def expirePassword(id: Int): IO[Unit] = {
    httpClient.expect[Unit](
      Request[IO](
        Method.PATCH,
        accountURL(id, Some("expire_password")),
        headers = authorizationHeaders,
      )
    )
  }

  def verifyToken(token: String): IO[TokenPayload] = for {
    payloadStr  <- verifier.verify(token)
    payloadJson <- IO(readFromString[TokenPayload](payloadStr))
  } yield payloadJson
}
