package helper

import parsing.types.Competence

trait FakeCompetences {
  implicit def fakeCompetences: Seq[Competence] = Seq(
    Competence("develop-visions", "Develop Visions", "...", "Develop Visions", "..."),
    Competence("analyze-domains", "Analyze Domains", "...", "Analyze Domains", "..."),
    Competence("model-systems", "Model Systems", "...", "Model Systems", "..."),
  )
}
