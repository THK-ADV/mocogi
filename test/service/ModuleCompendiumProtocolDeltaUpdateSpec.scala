package service

import database._
import models.{MetadataProtocol, ModuleCompendiumProtocol}
import org.scalatest.wordspec.AnyWordSpec
import parsing.types.{Content, ParsedWorkload, Participants}
import validator.Workload

import java.util.UUID

final class ModuleCompendiumProtocolDeltaUpdateSpec extends AnyWordSpec {
  import ModuleCompendiumProtocolDeltaUpdate.deltaUpdate

  private val existing = ModuleCompendiumProtocol(
    MetadataProtocol(
      "title",
      "abbrev",
      "moduleType",
      0.0,
      "language",
      0,
      "season",
      ParsedWorkload(0, 0, 0, 0, 0, 0),
      "status",
      "location",
      None,
      None,
      List("a"),
      List("a"),
      AssessmentMethodsOutput(
        List(AssessmentMethodEntryOutput("method", None, Nil)),
        Nil
      ),
      PrerequisitesOutput(None, None),
      POOutput(
        List(POMandatoryOutput("po1", None, List(1), Nil)),
        Nil
      ),
      Nil,
      Nil,
      Nil
    ),
    Content(
      "de_learningOutcome",
      "de_content",
      "de_teachingAndLearningMethods",
      "de_recommendedReading",
      "de_particularities"
    ),
    Content(
      "en_learningOutcome",
      "en_content",
      "en_teachingAndLearningMethods",
      "en_recommendedReading",
      "en_particularities"
    )
  )

  "A Module Compendium Protocol Delta Update" should {
    "update a module compendium by keys" in {
      import monocle.syntax.all._

      val newP = existing
        .focus(_.metadata.title)
        .replace("new title")
        .focus(_.metadata.ects)
        .replace(1.0)
        .focus(_.metadata.moduleManagement)
        .modify(xs => xs ::: List("b"))
        .focus(_.metadata.po.mandatory)
        .modify(xs =>
          xs ::: List(
            POMandatoryOutput("po2", Some("spec"), List(1, 2, 3), Nil)
          )
        )
        .focus(_.metadata.participants)
        .replace(Some(Participants(0, 10)))
      val (updated, updatedKeys) = deltaUpdate(existing, newP, None, Set.empty)
      assert(updatedKeys.size == 5)
      assert(updatedKeys.contains("metadata.title"))
      assert(updatedKeys.contains("metadata.ects"))
      assert(updatedKeys.contains("metadata.moduleManagement"))
      assert(updatedKeys.contains("metadata.po.mandatory"))
      assert(updatedKeys.contains("metadata.participants"))
      assert(updated.metadata.title == "new title")
      assert(updated.metadata.ects == 1.0)
      assert(updated.metadata.moduleManagement == List("a", "b"))
      assert(
        updated.metadata.po.mandatory == List(
          POMandatoryOutput("po1", None, List(1), Nil),
          POMandatoryOutput("po2", Some("spec"), List(1, 2, 3), Nil)
        )
      )
      assert(updated.metadata.participants.contains(Participants(0, 10)))
    }

    "undo update if its changed to the origin value" in {
      import monocle.syntax.all._
      val existing0 = existing
        .focus(_.metadata.title)
        .replace("new title")
      val newP = existing
        .focus(_.metadata.title)
        .replace("title")
        .focus(_.metadata.abbrev)
        .replace("new abbrev")
      val (updated, updatedKeys) = deltaUpdate(
        existing0,
        newP,
        Some(
          ModuleCompendiumOutput(
            "",
            MetadataOutput(
              UUID.randomUUID(),
              "title",
              existing0.metadata.abbrev,
              existing0.metadata.moduleType,
              existing0.metadata.ects,
              existing0.metadata.language,
              existing0.metadata.duration,
              existing0.metadata.season,
              Workload(
                existing0.metadata.workload.lecture,
                existing0.metadata.workload.seminar,
                existing0.metadata.workload.practical,
                existing0.metadata.workload.exercise,
                existing0.metadata.workload.projectSupervision,
                existing0.metadata.workload.projectWork,
                0,
                0
              ),
              existing0.metadata.status,
              existing0.metadata.location,
              existing0.metadata.participants,
              existing0.metadata.moduleRelation,
              existing0.metadata.moduleManagement,
              existing0.metadata.lecturers,
              existing0.metadata.assessmentMethods,
              existing0.metadata.prerequisites,
              existing0.metadata.po,
              existing0.metadata.competences,
              existing0.metadata.globalCriteria,
              existing0.metadata.taughtWith
            ),
            existing0.deContent,
            existing0.enContent
          )
        ),
        Set("metadata.title", "metadata.language")
      )
      assert(updatedKeys.size == 2)
      assert(updatedKeys.contains("metadata.abbrev"))
      assert(updatedKeys.contains("metadata.language"))
      assert(updated.metadata.title == "title")
      assert(updated.metadata.abbrev == "new abbrev")
    }
  }
}
