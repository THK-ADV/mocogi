package parsing.types

import monocle.Traversal
import monocle.macros.GenLens
import play.api.libs.json.{Format, Json}

case class Content(
    learningOutcome: String,
    content: String,
    teachingAndLearningMethods: String,
    recommendedReading: String,
    particularities: String
)

object Content {
  implicit def format: Format[Content] = Json.format

  final implicit class Ops(private val self: Content) extends AnyVal {
    private def trimAllProperties = Traversal
      .applyN(
        GenLens[Content](_.learningOutcome),
        GenLens[Content](_.content),
        GenLens[Content](_.teachingAndLearningMethods),
        GenLens[Content](_.recommendedReading),
        GenLens[Content](_.particularities)
      )
      .modify(_.trim)

    def normalize(): Content = trimAllProperties.apply(self)
  }
}
