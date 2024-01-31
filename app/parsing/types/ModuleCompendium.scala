package parsing.types

import monocle.Monocle.toAppliedFocusOps
import monocle.Traversal
import monocle.macros.GenLens
import play.api.libs.json.{Json, Writes}
import validator.Metadata

case class ModuleCompendium(
    metadata: Metadata,
    deContent: Content,
    enContent: Content
)

object ModuleCompendium {

  implicit def writes: Writes[ModuleCompendium] = Json.writes

  final implicit class Ops(private val self: ModuleCompendium) extends AnyVal {
    private def string = Traversal
      .applyN(
        GenLens[ModuleCompendium](_.metadata.title),
        GenLens[ModuleCompendium](_.metadata.abbrev)
      )
      .modify(_.trim)

    private def prerequisites = Traversal
      .applyN(
        GenLens[ModuleCompendium](
          _.metadata.prerequisites.recommended
        ),
        GenLens[ModuleCompendium](
          _.metadata.prerequisites.required
        )
      )
      .modify(
        _.map(
          _.focus(_.text)
            .modify(_.trim)
            .focus(_.modules)
            .modify(_.sortBy(_.id))
            .focus(_.pos)
            .modify(_.sortBy(_.id))
        )
      )

    private def assessmentMethods = Traversal
      .applyN(
        GenLens[ModuleCompendium](
          _.metadata.assessmentMethods.mandatory
        ),
        GenLens[ModuleCompendium](
          _.metadata.assessmentMethods.optional
        )
      )
      .modify(
        _.map(_.focus(_.precondition).modify(_.sortBy(_.id)))
          .sortBy(_.method.id)
      )

    private def poMandatory =
      GenLens[ModuleCompendium](_.metadata.validPOs.mandatory)
        .modify(
          _.map(
            _.focus(_.recommendedSemester)
              .modify(_.sorted)
              .focus(_.recommendedSemesterPartTime)
              .modify(_.sorted)
          ).sortBy(_.po.id)
        )

    private def poOptional =
      GenLens[ModuleCompendium](_.metadata.validPOs.optional)
        .modify(
          _.map(
            _.focus(_.recommendedSemester)
              .modify(_.sorted)
          ).sortBy(_.po.id)
        )

    private def identities = Traversal
      .applyN(
        GenLens[ModuleCompendium](_.metadata.responsibilities.moduleManagement),
        GenLens[ModuleCompendium](_.metadata.responsibilities.lecturers)
      )
      .modify(_.sortBy(_.id))

    private def competences =
      GenLens[ModuleCompendium](_.metadata.competences)
        .modify(_.sortBy(_.id))

    private def globalCriteria =
      GenLens[ModuleCompendium](_.metadata.globalCriteria)
        .modify(_.sortBy(_.id))

    private def taughtWith =
      GenLens[ModuleCompendium](_.metadata.taughtWith).modify(_.sortBy(_.id))

    private def content = Traversal
      .applyN(
        GenLens[ModuleCompendium](_.deContent),
        GenLens[ModuleCompendium](_.enContent)
      )
      .modify(_.normalize())

    def normalize() =
      (string andThen
        prerequisites andThen
        assessmentMethods andThen
        poMandatory andThen
        poOptional andThen
        identities andThen
        competences andThen
        globalCriteria andThen
        taughtWith andThen
        content) apply self
  }
}
