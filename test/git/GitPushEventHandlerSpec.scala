package git

import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDateTime

final class GitPushEventHandlerSpec extends AnyWordSpec {

  import git.webhook.GitPushEventHandler.removeModuleChanges

  "A GitPushEventHandler" should {
    "remove module changes" in {
      val changes = GitChanges[List[GitFilePath]](
        List(
          GitFilePath("modules/1.mc"),
          GitFilePath("modules/2.mc"),
          GitFilePath("core/1.mc")
        ),
        List(GitFilePath("core/2.mc")),
        List(GitFilePath("modules/3.mc"), GitFilePath("core/4.mc")),
        "commidId",
        LocalDateTime.now()
      )
      val res = removeModuleChanges(changes, "modules")
      assert(res.added == List(GitFilePath("core/1.mc")))
      assert(res.modified == List(GitFilePath("core/2.mc")))
      assert(res.removed == List(GitFilePath("core/4.mc")))
    }
  }
}
