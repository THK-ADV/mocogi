package helper

import parsing.types.GlobalCriteria

trait FakeGlobalCriteria {
  implicit def fakeGlobalCriteria: Seq[GlobalCriteria] = Seq(
    GlobalCriteria(
      "internationalization",
      "Internationalisierung",
      "...",
      "Internationalization",
      "..."
    ),
    GlobalCriteria(
      "interdisciplinarity",
      "Interdisziplinarität",
      "...",
      "Interdisciplinarity",
      "..."
    ),
    GlobalCriteria(
      "digitization",
      "Digitalisierung",
      "...",
      "Digitization",
      "..."
    )
  )
}
