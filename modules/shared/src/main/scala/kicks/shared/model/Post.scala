package kicks.shared.model

case class Thread(
  root: Post,
  children: Vector[Thread],
)

case class Post(
  id: Int,
  text: String,
)
