package service

import cats.data.NonEmptyList
import models._
import models.core.ExamPhase
import org.scalatest.wordspec.AnyWordSpec
import parsing.types.{ModuleContent, ModuleParticipants}

import java.util.UUID

final class ModuleProtocolDiffSpec extends AnyWordSpec {
  import ModuleProtocolDiff.diff

  private val existing = ModuleProtocol(
    None,
    MetadataProtocol(
      "title",
      "abbrev",
      "moduleType",
      0.0,
      "language",
      0,
      "season",
      ModuleWorkload(0, 0, 0, 0, 0, 0, 0, 0),
      "status",
      "location",
      None,
      None,
      NonEmptyList.one("a"),
      NonEmptyList.one("a"),
      ModuleAssessmentMethodsProtocol(
        List(ModuleAssessmentMethodEntryProtocol("method", None, Nil)),
        Nil
      ),
      Examiner.NN,
      NonEmptyList.one(ExamPhase.none.id),
      ModulePrerequisitesProtocol(None, None),
      ModulePOProtocol(
        List(ModulePOMandatoryProtocol("po1", None, List(1))),
        Nil
      ),
      Nil,
      Nil,
      Nil
    ),
    ModuleContent(
      "de_learningOutcome",
      "de_content",
      "de_teachingAndLearningMethods",
      "de_recommendedReading",
      "de_particularities"
    ),
    ModuleContent(
      "en_learningOutcome",
      "en_content",
      "en_teachingAndLearningMethods",
      "en_recommendedReading",
      "en_particularities"
    )
  )

  "A Module Protocol Diff" should {
    "update a module by keys" in {
      import monocle.syntax.all._

      val newP = existing
        .focus(_.metadata.title)
        .replace("new title")
        .focus(_.metadata.ects)
        .replace(1.0)
        .focus(_.metadata.moduleManagement)
        .modify(_ ::: NonEmptyList.one("b"))
        .focus(_.metadata.po.mandatory)
        .modify(
          _ ::: List(
            ModulePOMandatoryProtocol("po2", Some("spec"), List(1, 2, 3))
          )
        )
        .focus(_.metadata.participants)
        .replace(Some(ModuleParticipants(0, 10)))
        .focus(_.metadata.examiner.first)
        .replace("ald")
        .focus(_.metadata.examPhases)
        .modify(_ ::: NonEmptyList.one("a"))
      val (updated, updatedKeys) = diff(existing, newP, None, Set.empty)
      assert(updatedKeys.size == 7)
      assert(updatedKeys.contains("metadata.title"))
      assert(updatedKeys.contains("metadata.ects"))
      assert(updatedKeys.contains("metadata.moduleManagement"))
      assert(updatedKeys.contains("metadata.po.mandatory"))
      assert(updatedKeys.contains("metadata.participants"))
      assert(updatedKeys.contains("metadata.examiner.first"))
      assert(updated.metadata.title == "new title")
      assert(updated.metadata.ects == 1.0)
      assert(updated.metadata.moduleManagement == NonEmptyList.of("a", "b"))
      assert(
        updated.metadata.po.mandatory == List(
          ModulePOMandatoryProtocol("po1", None, List(1)),
          ModulePOMandatoryProtocol("po2", Some("spec"), List(1, 2, 3))
        )
      )
      assert(updated.metadata.participants.contains(ModuleParticipants(0, 10)))
      assert(updated.metadata.examiner.first == "ald")
      assert(
        updated.metadata.examPhases == existing.metadata.examPhases.append("a")
      )
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
      val (updated, updatedKeys) = diff(
        existing0,
        newP,
        Some(
          ModuleProtocol(
            Some(UUID.randomUUID()),
            MetadataProtocol(
              "title",
              existing0.metadata.abbrev,
              existing0.metadata.moduleType,
              existing0.metadata.ects,
              existing0.metadata.language,
              existing0.metadata.duration,
              existing0.metadata.season,
              ModuleWorkload(
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
              existing0.metadata.examiner,
              existing0.metadata.examPhases,
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
