package parsing.types

import models.Metadata
import monocle.macros.GenLens
import monocle.syntax.all.focus
import monocle.Traversal
import play.api.libs.json.Json
import play.api.libs.json.Writes

case class Module(
    metadata: Metadata,
    deContent: ModuleContent,
    enContent: ModuleContent
)

object Module {

  implicit def writes: Writes[Module] = Json.writes

  final implicit class Ops(private val self: Module) extends AnyVal {
    private def string = Traversal
      .applyN(
        GenLens[Module](_.metadata.title),
        GenLens[Module](_.metadata.abbrev)
      )
      .modify(_.trim)

    private def prerequisites = Traversal
      .applyN(
        GenLens[Module](
          _.metadata.prerequisites.recommended
        ),
        GenLens[Module](
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
        GenLens[Module](
          _.metadata.assessmentMethods.mandatory
        )
      )
      .modify(
        _.map(_.focus(_.precondition).modify(_.sortBy(_.id)))
          .sortBy(_.method.id)
      )

    private def poMandatory =
      GenLens[Module](_.metadata.pos.mandatory)
        .modify(
          _.map(
            _.focus(_.recommendedSemester)
              .modify(_.sorted)
          ).sortBy(_.po.id)
        )

    private def poOptional =
      GenLens[Module](_.metadata.pos.optional)
        .modify(
          _.map(
            _.focus(_.recommendedSemester)
              .modify(_.sorted)
          ).sortBy(_.po.id)
        )

    private def identities = Traversal
      .applyN(
        GenLens[Module](_.metadata.responsibilities.moduleManagement),
        GenLens[Module](_.metadata.responsibilities.lecturers)
      )
      .modify(_.sortBy(_.id))

    private def competences =
      GenLens[Module](_.metadata.competences)
        .modify(_.sortBy(_.id))

    private def globalCriteria =
      GenLens[Module](_.metadata.globalCriteria)
        .modify(_.sortBy(_.id))

    private def taughtWith =
      GenLens[Module](_.metadata.taughtWith).modify(_.sortBy(_.id))

    private def content = Traversal
      .applyN(
        GenLens[Module](_.deContent),
        GenLens[Module](_.enContent)
      )
      .modify(_.normalize())

    def normalize(): Module =
      string
        .andThen(prerequisites)
        .andThen(assessmentMethods)
        .andThen(poMandatory)
        .andThen(poOptional)
        .andThen(identities)
        .andThen(competences)
        .andThen(globalCriteria)
        .andThen(taughtWith)
        .andThen(content)
        .apply(self)
  }
}
