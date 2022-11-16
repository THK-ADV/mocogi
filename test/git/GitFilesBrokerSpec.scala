package git

import git.GitFilesBroker.{Changes, core, modules, split}
import org.scalatest.OptionValues
import org.scalatest.wordspec.AnyWordSpec

final class GitFilesBrokerSpec extends AnyWordSpec with OptionValues {

  "A GitFilesBroker" should {
    "split git files to paths if they are empty" in {
      val changes1: Changes = GitChanges(Nil, Nil, Nil, "commitId")
      assert(split(changes1).isEmpty)
      val changes2: Changes = GitChanges(
        List((GitFilePath("modules/added1"), GitFileContent(""))),
        Nil,
        Nil,
        "commitId"
      )
      val mods = split(changes2)
      assert(mods.size == 1)
      assert(mods.get(modules).value.added.nonEmpty)
      assert(mods.get(modules).value.modified.isEmpty)
      assert(mods.get(modules).value.removed.isEmpty)
      val changes3: Changes = GitChanges(
        List((GitFilePath("core/added1"), GitFileContent(""))),
        Nil,
        Nil,
        "commitId"
      )
      val cr = split(changes3)
      assert(cr.size == 1)
      assert(cr.get(core).value.added.nonEmpty)
      assert(cr.get(core).value.modified.isEmpty)
      assert(cr.get(core).value.removed.isEmpty)
    }

    "split git files to paths" in {
      val changes = GitChanges(
        List(
          (GitFilePath("modules/added1"), GitFileContent("")),
          (GitFilePath("modules/added2"), GitFileContent("")),
          (GitFilePath("core/people.yaml"), GitFileContent("")),
          (GitFilePath("core/data.yaml"), GitFileContent(""))
        ),
        List(
          (GitFilePath("modules/modified1"), GitFileContent("")),
          (GitFilePath("core/stuff.yaml"), GitFileContent(""))
        ),
        List(GitFilePath("modules/removed1")),
        "commitId"
      )

      val res = split(changes)
      assert(res.size == 2)
      val modules = res.get("modules").value
      assert(modules.commitId == "commitId")

      assert(
        modules.added == List(
          (GitFilePath("added1"), GitFileContent("")),
          (GitFilePath("added2"), GitFileContent(""))
        )
      )
      assert(
        modules.modified == List(
          (GitFilePath("modified1"), GitFileContent(""))
        )
      )
      assert(
        modules.removed == List(
          GitFilePath("removed1")
        )
      )

      val core = res.get("core").value
      assert(core.commitId == "commitId")

      assert(
        core.added == List(
          (GitFilePath("people.yaml"), GitFileContent("")),
          (GitFilePath("data.yaml"), GitFileContent(""))
        )
      )
      assert(
        core.modified == List(
          (GitFilePath("stuff.yaml"), GitFileContent(""))
        )
      )
      assert(core.removed.isEmpty)
    }
  }
}
