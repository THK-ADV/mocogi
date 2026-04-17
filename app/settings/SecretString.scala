package settings

/** Opaque string for secrets (tokens); use [[unwrap]] only at IO boundaries. */
opaque type SecretString = String

object SecretString:
  def unsafe(raw: String): SecretString = raw

  def unwrap(s: SecretString): String = s
