package auth

enum Token:
  def username: String
  def roles: Set[String]

  def hasRole(role: Role) = roles.contains(role.label)

  case UserToken(firstname: String, lastname: String, username: String, email: String, roles: Set[String])
  case ServiceToken(username: String, roles: Set[String])
