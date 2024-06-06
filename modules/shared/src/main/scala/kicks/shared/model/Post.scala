package kicks.shared.model

case class Post(
  id: Int,
  text: String,
  children: Vector[Post],
)
