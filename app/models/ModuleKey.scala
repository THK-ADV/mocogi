package models

object ModuleKey {
  def normalizeKeyValue(key: String): String =
    if (key.startsWith("deContent") || key.startsWith("enContent")) "content" + key.dropWhile(_ != '.')
    else key
}
