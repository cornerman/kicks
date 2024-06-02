package kicks.http

case class ServerConfig(
  jdbcUrl: String,
  frontendDistributionPath: String,
  authnIssuerUrl: String,
  authnAdminUrl: String,
  authnAdminUsername: String,
  authnAdminPassword: String,
  authnAudience: String,
) {
  override def toString(): String = {
    val fields = productElementNames.zip(productIterator).map { (name, value) =>
      val isSecret       = name.toLowerCase.contains("password") || name.toLowerCase.contains("secret")
      val sanitizedValue = if (isSecret) "***" else value
      s"$name = $sanitizedValue"
    }
    s"${productPrefix}(${fields.mkString(", ")})"
  }
}

object ServerConfig {
  def fromEnvOrThrow() = ServerConfig(
    jdbcUrl = sys.env("DATABASE_URL"),
    frontendDistributionPath = sys.env("FRONTEND_DISTRIBUTION_PATH"),
    authnIssuerUrl = sys.env("AUTHN_ISSUER_URL"),
    authnAdminUrl = sys.env("AUTHN_ADMIN_URL"),
    authnAdminUsername = sys.env("AUTHN_ADMIN_USERNAME"),
    authnAdminPassword = sys.env("AUTHN_ADMIN_PASSWORD"),
    authnAudience = sys.env("AUTHN_AUDIENCE"),
  )
}
