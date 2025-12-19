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
) {
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

  private def taughtWith =
    GenLens[Module](_.metadata.taughtWith).modify(_.sortBy(_.id))

  private def content = Traversal
    .applyN(
      GenLens[Module](_.deContent),
      GenLens[Module](_.enContent)
    )
    .modify(_.normalized())

  def normalized(): Module =
    string
      .andThen(prerequisites)
      .andThen(assessmentMethods)
      .andThen(poMandatory)
      .andThen(poOptional)
      .andThen(identities)
      .andThen(taughtWith)
      .andThen(content)
      .apply(this)
}

object Module {
  given Writes[Module] = Json.writes
}
