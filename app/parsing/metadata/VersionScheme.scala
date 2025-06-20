package parsing.metadata

case class VersionScheme(number: Double, label: String)

object VersionScheme {
  def default = VersionScheme(1, "s")
}
