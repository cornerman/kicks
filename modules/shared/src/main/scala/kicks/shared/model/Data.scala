package kicks.shared.model

import kicks.shared.JsonPickler.ReadWriter

case class Thread(
  root: Post,
  children: Vector[Thread],
) derives ReadWriter

case class Post(
  id: Int,
  text: String,
) derives ReadWriter

object Post {
  case class Creator(
    text: String
  ) derives ReadWriter
}
