package helper

import models.core.ModuleGlobalCriteria

trait FakeGlobalCriteria {
  implicit def fakeGlobalCriteria: Seq[ModuleGlobalCriteria] = Seq(
    ModuleGlobalCriteria(
      "internationalization",
      "Internationalisierung",
      "...",
      "Internationalization",
      "..."
    ),
    ModuleGlobalCriteria(
      "interdisciplinarity",
      "Interdisziplinarität",
      "...",
      "Interdisciplinarity",
      "..."
    ),
    ModuleGlobalCriteria(
      "digitization",
      "Digitalisierung",
      "...",
      "Digitization",
      "..."
    )
  )
}
