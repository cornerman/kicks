package kicks.http.auth

enum AuthUser {
  case Anon
  case User(id: String)

}
