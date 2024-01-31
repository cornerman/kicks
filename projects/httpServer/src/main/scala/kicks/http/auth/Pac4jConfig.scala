package kicks.http.auth

import cats.effect.IO
import org.http4s.ResponseCookie
import org.pac4j.core.client.Clients
import org.pac4j.core.config.{Config, ConfigFactory}
import org.pac4j.http.client.indirect.FormClient
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator
import org.pac4j.http4s.{DefaultHttpActionAdapter, Http4sCacheSessionStore, SessionConfig}

import scala.concurrent.duration.*

// https: //github.com/pac4j/http4s-pac4j-demo/blob/master/src/main/scala/com/test/DemoConfigFactory.scala
object Pac4jConfig {
  def clientConfig(callbackUrl: String, loginFormUrl: String): Config = {
    val formClient = new FormClient(loginFormUrl, new SimpleTestUsernamePasswordAuthenticator())

//    val oidcClient = {
//      val oidcConfiguration = new OidcConfiguration()
//      oidcConfiguration.setClientId("343992089165-sp0l1km383i8cbm2j5nn20kbk5dk8hor.apps.googleusercontent.com")
//      oidcConfiguration.setSecret("uR3D8ej1kIRPbqAFaxIE3HWh")
//      oidcConfiguration.setDiscoveryURI("https://accounts.google.com/.well-known/openid-configuration")
//      oidcConfiguration.setUseNonce(true)
//      oidcConfiguration.addCustomParam("prompt", "consent")
//      val oidcClient = new OidcClient(oidcConfiguration)
//
//      val authorizationGenerator = new AuthorizationGenerator {
//        override def generate(context: WebContext, sessionStore: SessionStore, profile: UserProfile): Optional[UserProfile] = {
//          profile.addRole("ROLE_ADMIN")
//          Optional.of(profile)
//        }
//      }
//      oidcClient.setAuthorizationGenerator(authorizationGenerator)
//      oidcClient
//    }

//    val saml2Client = {
//      val cfg = new SAML2Configuration("file:saml-keystore.p12",
//        "pac4j-demo-passwd",
//        "pac4j-demo-passwd",
//        "resource:metadata-okta.xml")
//      cfg.setMaximumAuthenticationLifetime(3600)
//      cfg.setServiceProviderEntityId("http://localhost:8080/callback?client_name=SAML2Client")
//      cfg.setServiceProviderMetadataPath("saml-sp-metadata.xml")
//      new SAML2Client(cfg)
//    }

//    val facebookClient =
//      new FacebookClient("145278422258960", "be21409ba8f39b5dae2a7de525484da8")

    val config = new Config(callbackUrl, formClient)
    // config.addAuthorizer("admin", new RequireAnyRoleAuthorizer[_ <: CommonProfile]("ROLE_ADMIN"))
    // config.addAuthorizer("custom", new CustomAuthorizer)
    config.setHttpActionAdapter(new DefaultHttpActionAdapter[IO]) // <-- Render a nicer page
    config.setSessionStore(new Http4sCacheSessionStore[IO]())
    //    config.setSessionStoreFactory(_ => new Http4sCookieSessionStore[F]{})
    config
  }

  val sessionConfig = SessionConfig(
    cookieName = "session",
    mkCookie = ResponseCookie(_, _, path = Some("/")),
    secret = List.fill(16)(0xff.toByte), // FIXME
    maxAge = 5.minutes,
  )
}
