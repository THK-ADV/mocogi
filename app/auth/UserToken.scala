package auth

case class UserToken(
    firstname: String,
    lastname: String,
    username: String,
    email: String,
    roles: Set[String]
)
