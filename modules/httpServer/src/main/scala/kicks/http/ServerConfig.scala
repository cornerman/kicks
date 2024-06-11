package kicks.http

import kicks.shared.AppConfig
import monocle.syntax.all.*

case class ServerConfig(
  jdbcUrl: String,
  frontendDistributionPath: String,
  authnIssuerUrl: String,
  authnAdminUrl: String,
  authnAdminUsername: String,
  authnAdminPassword: String,
  authnAudience: String,
) {
  def appConfig = AppConfig(
    authnUrl = authnIssuerUrl
  )

  override def toString: String =
    val sanitized = this.focus(_.authnAdminPassword).replace("***")
    productPrefix + Tuple.fromProduct(sanitized).toString
}

object ServerConfig {
  def fromEnvOrThrow(): ServerConfig = ServerConfig(
    jdbcUrl = sys.env("DATABASE_URL"),
    frontendDistributionPath = sys.env("FRONTEND_DISTRIBUTION_PATH"),
    authnIssuerUrl = sys.env("AUTHN_ISSUER_URL"),
    authnAdminUrl = sys.env("AUTHN_ADMIN_URL"),
    authnAdminUsername = sys.env("AUTHN_ADMIN_USERNAME"),
    authnAdminPassword = sys.env("AUTHN_ADMIN_PASSWORD"),
    authnAudience = sys.env("AUTHN_AUDIENCE"),
  )
}
