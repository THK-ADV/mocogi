package models

import monocle.Traversal
import monocle.macros.GenLens
import monocle.syntax.all._
import parsing.types.Content
import play.api.libs.json.{Format, Json}

case class ModuleCompendiumProtocol(
    metadata: MetadataProtocol,
    deContent: Content,
    enContent: Content
)

object ModuleCompendiumProtocol {

  implicit def format: Format[ModuleCompendiumProtocol] = Json.format

  final implicit class Ops(private val self: ModuleCompendiumProtocol)
      extends AnyVal {
    private def string = Traversal
      .applyN(
        GenLens[ModuleCompendiumProtocol](_.metadata.title),
        GenLens[ModuleCompendiumProtocol](_.metadata.abbrev)
      )
      .modify(_.trim)

    private def prerequisites = Traversal
      .applyN(
        GenLens[ModuleCompendiumProtocol](
          _.metadata.prerequisites.recommended
        ),
        GenLens[ModuleCompendiumProtocol](
          _.metadata.prerequisites.required
        )
      )
      .modify(
        _.map(
          _.focus(_.text)
            .modify(_.trim)
            .focus(_.modules)
            .modify(_.sorted)
            .focus(_.pos)
            .modify(_.sorted)
        )
      )

    private def assessmentMethods = Traversal
      .applyN(
        GenLens[ModuleCompendiumProtocol](
          _.metadata.assessmentMethods.mandatory
        ),
        GenLens[ModuleCompendiumProtocol](
          _.metadata.assessmentMethods.optional
        )
      )
      .modify(_.map(_.focus(_.precondition).modify(_.sorted)).sortBy(_.method))

    private def poMandatory =
      GenLens[ModuleCompendiumProtocol](_.metadata.po.mandatory)
        .modify(
          _.map(
            _.focus(_.recommendedSemester)
              .modify(_.sorted)
              .focus(_.recommendedSemesterPartTime)
              .modify(_.sorted)
          ).sortBy(_.po)
        )

    private def poOptional =
      GenLens[ModuleCompendiumProtocol](_.metadata.po.optional)
        .modify(
          _.map(
            _.focus(_.recommendedSemester)
              .modify(_.sorted)
          ).sortBy(_.po)
        )

    private def strings = Traversal
      .applyN(
        GenLens[ModuleCompendiumProtocol](_.metadata.moduleManagement),
        GenLens[ModuleCompendiumProtocol](_.metadata.lecturers),
        GenLens[ModuleCompendiumProtocol](_.metadata.competences),
        GenLens[ModuleCompendiumProtocol](_.metadata.globalCriteria)
      )
      .modify(_.sorted)

    private def ids =
      GenLens[ModuleCompendiumProtocol](_.metadata.taughtWith).modify(_.sorted)

    private def content = Traversal
      .applyN(
        GenLens[ModuleCompendiumProtocol](_.deContent),
        GenLens[ModuleCompendiumProtocol](_.enContent)
      )
      .modify(_.normalize())

    def normalize() = string
      .andThen(prerequisites)
      .andThen(strings)
      .andThen(assessmentMethods)
      .andThen(poMandatory)
      .andThen(poOptional)
      .andThen(ids)
      .andThen(content)
      .apply(self)
  }
}
