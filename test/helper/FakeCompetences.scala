package helper

import models.core.ModuleCompetence

trait FakeCompetences {
  implicit def fakeCompetences: Seq[ModuleCompetence] = Seq(
    ModuleCompetence("develop-visions", "Develop Visions", "...", "Develop Visions", "..."),
    ModuleCompetence("analyze-domains", "Analyze Domains", "...", "Analyze Domains", "..."),
    ModuleCompetence("model-systems", "Model Systems", "...", "Model Systems", "..."),
  )
}
