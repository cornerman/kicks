package kicks.webapp.state

import funstack.client.core.auth.User

case class Auth(
  user: Option[User],
)

case class State(
  auth: Auth,
)
