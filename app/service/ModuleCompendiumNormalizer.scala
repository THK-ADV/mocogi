package service

import models.ModuleCompendiumProtocol
import parsing.types.{Content, ModuleCompendium}

object ModuleCompendiumNormalizer {
  private def normalizeContent(c: Content) =
    Content(
      c.learningOutcome.trim,
      c.content.trim,
      c.teachingAndLearningMethods.trim,
      c.recommendedReading.trim,
      c.particularities.trim
    )

  def normalize(
    p: ModuleCompendiumProtocol
  ): ModuleCompendiumProtocol = ModuleCompendiumProtocol(
    p.metadata.copy(
      title = p.metadata.title.trim,
      abbrev = p.metadata.abbrev.trim,
      prerequisites = p.metadata.prerequisites.copy(
        recommended = p.metadata.prerequisites.recommended.map(e =>
          e.copy(text = e.text.trim)
        ),
        required =
          p.metadata.prerequisites.required.map(e => e.copy(text = e.text.trim))
      )
    ),
    normalizeContent(p.deContent),
    normalizeContent(p.enContent)
  )

  def normalize(
    p: ModuleCompendium
  ): ModuleCompendium = ModuleCompendium(
    p.metadata.copy(
      title = p.metadata.title.trim,
      abbrev = p.metadata.abbrev.trim,
      prerequisites = p.metadata.prerequisites.copy(
        recommended = p.metadata.prerequisites.recommended.map(e =>
          e.copy(text = e.text.trim)
        ),
        required =
          p.metadata.prerequisites.required.map(e => e.copy(text = e.text.trim))
      )
    ),
    normalizeContent(p.deContent),
    normalizeContent(p.enContent)
  )
}
