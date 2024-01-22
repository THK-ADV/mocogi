package printing

sealed trait PrintingLanguage {
  def id: String

  def fold[A](de: => A, en: => A): A =
    this match {
      case PrintingLanguage.German  => de
      case PrintingLanguage.English => en
    }

  override def toString = fold("german", "english")
}

object PrintingLanguage {
  case object German extends PrintingLanguage {
    override def id: String = "de"
  }

  case object English extends PrintingLanguage {
    override def id: String = "en"
  }

  def apply(str: String): Option[PrintingLanguage] =
    str.toLowerCase match {
      case "de" => Some(German)
      case "en" => Some(English)
      case _    => None
    }

  def all(): Seq[PrintingLanguage] = List(German, English)
}
