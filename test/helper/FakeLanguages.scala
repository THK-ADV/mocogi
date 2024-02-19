package helper

import models.core.ModuleLanguage

trait FakeLanguages {
  implicit def fakeLanguages: Seq[ModuleLanguage] = Seq(
    ModuleLanguage("de", "Deutsch", "--"),
    ModuleLanguage("en", "Englisch", "--"),
    ModuleLanguage("de_en", "Deutsch und Englisch", "--")
  )
}
