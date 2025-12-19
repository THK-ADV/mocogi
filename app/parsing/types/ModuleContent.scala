package parsing.types

import monocle.macros.GenLens
import monocle.Traversal
import play.api.libs.json.Format
import play.api.libs.json.Json

case class ModuleContent(
    learningOutcome: String,
    content: String,
    teachingAndLearningMethods: String,
    recommendedReading: String,
    particularities: String
) {
  private def trimAllProperties = Traversal
    .applyN(
      GenLens[ModuleContent](_.learningOutcome),
      GenLens[ModuleContent](_.content),
      GenLens[ModuleContent](_.teachingAndLearningMethods),
      GenLens[ModuleContent](_.recommendedReading),
      GenLens[ModuleContent](_.particularities)
    )
    .modify(_.trim)

  def normalized(): ModuleContent = trimAllProperties.apply(this)
}

object ModuleContent {
  given Format[ModuleContent] = Json.format
}
