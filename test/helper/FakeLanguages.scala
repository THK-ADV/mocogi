package helper

import parsing.types.Language

trait FakeLanguages {
  implicit def fakeLanguages: Seq[Language] = Seq(
    Language("de", "Deutsch", "--"),
    Language("en", "Englisch", "--"),
    Language("de_en", "Deutsch und Englisch", "--")
  )
}
