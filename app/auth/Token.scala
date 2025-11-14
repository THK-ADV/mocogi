package auth

enum Token:
  def username: String
  def roles: Set[String]

  case UserToken(firstname: String, lastname: String, username: String, email: String, roles: Set[String])
  case ServiceToken(username: String, roles: Set[String])
