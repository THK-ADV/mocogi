package git

import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

import java.io.File
import scala.io.Source

final class GitMergeEventHandlingActorSpec extends AnyWordSpec {
  import git.webhook.GitMergeEventHandlingActor._

  def parseJson(name: String): JsValue = {
    val s = Source.fromFile(new File(s"test/git/$name"))
    val j = Json.parse(s.mkString)
    s.close()
    j
  }

  "A GitWebhookController" should {
    "parse action as a merge action" in {
      val openAction = parseJson("action-open.json")
      assert(!parseIsMerge(openAction).get)
      val mergeAction = parseJson("action-merge.json")
      assert(parseIsMerge(mergeAction).get)
    }

    "parse merge request id" in {
      val json = parseJson("action-open.json")
      assert(parseMergeRequestId(json).get == 44)
    }

    "parse source branch" in {
      val json = parseJson("action-open.json")
      assert(
        parseSourceBranch(
          json
        ).get == "kohls_d7aa0d5c-5cfb-4550-be69-7e587a93f213"
      )
    }

    "parse last commit id" in {
      val json = parseJson("action-open.json")
      assert(
        parseLastCommitId(
          json
        ).get == "5c30e60c40d214297460083a058896dc1c0b1504"
      )
    }
  }
}
