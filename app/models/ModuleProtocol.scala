package models

import monocle.Traversal
import monocle.macros.GenLens
import monocle.syntax.all._
import parsing.types.ModuleContent
import play.api.libs.json.{Format, Json}

import java.util.UUID

case class ModuleProtocol(
    id: Option[UUID],
    metadata: MetadataProtocol,
    deContent: ModuleContent,
    enContent: ModuleContent
)

object ModuleProtocol {

  implicit def format: Format[ModuleProtocol] = Json.format

  final implicit class Ops(private val self: ModuleProtocol) extends AnyVal {
    private def string = Traversal
      .applyN(
        GenLens[ModuleProtocol](_.metadata.title),
        GenLens[ModuleProtocol](_.metadata.abbrev)
      )
      .modify(_.trim)

    private def prerequisites = Traversal
      .applyN(
        GenLens[ModuleProtocol](
          _.metadata.prerequisites.recommended
        ),
        GenLens[ModuleProtocol](
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
        GenLens[ModuleProtocol](
          _.metadata.assessmentMethods.mandatory
        ),
        GenLens[ModuleProtocol](
          _.metadata.assessmentMethods.optional
        )
      )
      .modify(_.map(_.focus(_.precondition).modify(_.sorted)).sortBy(_.method))

    private def poMandatory =
      GenLens[ModuleProtocol](_.metadata.po.mandatory)
        .modify(
          _.map(
            _.focus(_.recommendedSemester)
              .modify(_.sorted)
          ).sortBy(_.po)
        )

    private def poOptional =
      GenLens[ModuleProtocol](_.metadata.po.optional)
        .modify(
          _.map(
            _.focus(_.recommendedSemester)
              .modify(_.sorted)
          ).sortBy(_.po)
        )

    private def lists = Traversal
      .applyN(
        GenLens[ModuleProtocol](_.metadata.competences),
        GenLens[ModuleProtocol](_.metadata.globalCriteria)
      )
      .modify(_.sorted)

    private def nels = Traversal
      .applyN(
        GenLens[ModuleProtocol](_.metadata.moduleManagement),
        GenLens[ModuleProtocol](_.metadata.lecturers)
      )
      .modify(_.sorted)

    private def ids =
      GenLens[ModuleProtocol](_.metadata.taughtWith).modify(_.sorted)

    private def content = Traversal
      .applyN(
        GenLens[ModuleProtocol](_.deContent),
        GenLens[ModuleProtocol](_.enContent)
      )
      .modify(_.normalize())

    def normalize() = string
      .andThen(prerequisites)
      .andThen(lists)
      .andThen(nels)
      .andThen(assessmentMethods)
      .andThen(poMandatory)
      .andThen(poOptional)
      .andThen(ids)
      .andThen(content)
      .apply(self)
  }
}
