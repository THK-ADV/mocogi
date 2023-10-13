package git

import models.MergeRequestId
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
      assert(parseMergeRequestId(json).get == MergeRequestId(44))
    }
  }
}
