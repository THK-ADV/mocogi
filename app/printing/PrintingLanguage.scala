package printing

sealed trait PrintingLanguage {
  def fold[A](de: => A, en: => A): A =
    this match {
      case PrintingLanguage.German  => de
      case PrintingLanguage.English => en
    }

  override def toString = fold("german", "english")
}

object PrintingLanguage {
  case object German extends PrintingLanguage
  case object English extends PrintingLanguage
}
