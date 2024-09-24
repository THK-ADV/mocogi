package models

object ModuleKey {
  // TODO: remove if language prefix is dropped
  def normalizeKeyValue(key: String): String =
    if (key.startsWith("deContent") || key.startsWith("enContent"))
      "content" + key.dropWhile(_ != '.')
    else key
}
