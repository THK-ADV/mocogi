package git

import models.{Branch, CommitId}
import org.scalatest.TryValues
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.io.Source

final class GitPushEventHandlingActorSpec extends AnyWordSpec with TryValues {
  import git.webhook.GitPushEventHandlingActor._

  def parseJson(name: String): JsValue = {
    val s = Source.fromFile(new File(s"test/git/$name"))
    val j = Json.parse(s.mkString)
    s.close()
    j
  }

  "A Git Push Event Handling Actor" should {
    "parse after commit" in {
      val draft = parseJson("push-in-draft.json")
      assert(
        parseAfterCommit(draft).get == CommitId(
          "f4c87fa7fbfaa65e4d6277833bb65ec93e1c9286"
        )
      )

      val main = parseJson("push-in-main.json")
      assert(
        parseAfterCommit(main).get == CommitId(
          "3d2a3763792b4ea95bde4cb3fad9ab344e65102e"
        )
      )
    }

    "parse target branch" in {
      val draft = parseJson("push-in-draft.json")
      assert(parseBranch(draft).get == Branch("draft"))

      val main = parseJson("push-in-main.json")
      assert(parseBranch(main).get == Branch("master"))
    }

    "parse files of last commit" in {
      val draft = parseJson("push-in-draft.json")
      val result1 = parseFilesOfLastCommit(
        draft,
        CommitId("f4c87fa7fbfaa65e4d6277833bb65ec93e1c9286")
      ).get
      assert(
        result1 == (Nil, List(
          GitFilePath("modules/23ce931b-4d27-47da-9158-3ab8979759cf.md")
        ), Nil, LocalDateTime
          .parse("2023-10-19T09:03:03+00:00", DateTimeFormatter.ISO_DATE_TIME))
      )

      val main = parseJson("push-in-main.json")
      val result2 = parseFilesOfLastCommit(
        main,
        CommitId("3d2a3763792b4ea95bde4cb3fad9ab344e65102e")
      ).get
      assert(
        result2 == (Nil, List(
          GitFilePath("modules/23ce931b-4d27-47da-9158-3ab8979759cf.md"),
          GitFilePath("modules/e37c5af9-6076-4f15-8c8b-d206b7091bc0.md")
        ), Nil, LocalDateTime
          .parse("2023-10-19T09:05:07+00:00", DateTimeFormatter.ISO_DATE_TIME))
      )
    }

    "separate modules from core entries" in {
      val config =
        GitConfig(None, "", None, "", 0, "", "", "modules", "core", "", "")
      val main = parseJson("push-in-main.json")
      val changes = parse(main).success.value._2
      assert(changes.added.isEmpty)
      assert(changes.removed.isEmpty)
      val (modules, cores) = changes.modified.partition(_.isModule(config))
      assert(
        modules == List(
          GitFilePath("modules/23ce931b-4d27-47da-9158-3ab8979759cf.md"),
          GitFilePath("modules/e37c5af9-6076-4f15-8c8b-d206b7091bc0.md")
        )
      )
      assert(cores.isEmpty)
      val moduleIds = changes.modified.map(_.moduleId(config))
      assert(
        moduleIds == List(
          Some(UUID.fromString("23ce931b-4d27-47da-9158-3ab8979759cf")),
          Some(UUID.fromString("e37c5af9-6076-4f15-8c8b-d206b7091bc0"))
        )
      )

      val mainCore = parseJson("push-in-main-core.json")
      val changes2 = parse(mainCore).success.value._2
      assert(changes2.added.isEmpty)
      assert(changes2.removed.isEmpty)
      val (modules2, cores2) = changes2.modified.partition(_.isModule(config))
      assert(
        cores2 == List(
          GitFilePath("core/status.yaml"),
          GitFilePath("core/location.yaml"),
          GitFilePath("core/lang.yaml"),
          GitFilePath("core/module_type.yaml"),
          GitFilePath("core/assessment.yaml"),
          GitFilePath("core/season.yaml"),
          GitFilePath("core/person.yaml"),
          GitFilePath("core/program.yaml"),
          GitFilePath("core/global_criteria.yaml"),
          GitFilePath("core/focus_area.yaml"),
          GitFilePath("core/po.yaml"),
          GitFilePath("core/competence.yaml"),
          GitFilePath("core/study_form.yaml"),
          GitFilePath("core/grade.yaml"),
          GitFilePath("core/faculty.yaml"),
          GitFilePath("core/specialization.yaml")
        )
      )
      assert(modules2.isEmpty)
      val moduleIds2 = changes2.modified.flatMap(_.moduleId(config))
      assert(moduleIds2.isEmpty)
    }
  }
}
