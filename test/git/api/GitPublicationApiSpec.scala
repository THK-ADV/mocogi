package git.api

import scala.concurrent.duration.*
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import git.*
import helper.FakeIdentities
import helper.GitHttpFixture
import models.core.Identity
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.*
import play.api.libs.ws.writeableOf_WsBody
import play.api.libs.ws.BodyWritable
import play.api.libs.ws.EmptyBody

final class GitPublicationApiSpec extends AnyFunSuite with Matchers with MockitoSugar with FakeIdentities {
  private given ExecutionContext    = ExecutionContext.parasitic
  private given GitRepositoryConfig =
    GitRepositoryConfig(Branch("main"), Branch("preview"), "modules", "catalogs", "companions")

  private val main    = CommitId("1" * 40)
  private val preview = CommitId("2" * 40)
  private val mr      = MergeRequestId(17)
  private val oldPath = "modules/00000000-0000-0000-0000-000000000001.md"
  private val newPath = "modules/00000000-0000-0000-0000-000000000002.md"
  private val diff    = Json.obj(
    "old_path"     -> oldPath,
    "new_path"     -> newPath,
    "new_file"     -> false,
    "renamed_file" -> true,
    "deleted_file" -> false,
    "diff"         -> "@@ -1 +1 @@\n-old\n+new"
  )

  private def await[A](future: Future[A]): A   = Await.result(future, 5.seconds)
  private def repository(http: GitHttpFixture) =
    new GitRepositoryService(http.client, http.config, summon[GitRepositoryConfig], summon[ExecutionContext])
  private def mergeRequests(http: GitHttpFixture) =
    new GitMergeRequestService(http.client, http.config, summon[ExecutionContext])
  private def changes(changes: JsValue = Json.arr(diff), sha: String = preview.value, overflow: Boolean = false) =
    Json.obj("changes" -> changes, "diff_refs" -> Json.obj("head_sha" -> sha), "overflow" -> overflow)

  test("compare requests both exact revisions and a direct tree comparison, preserving both rename paths") {
    val http    = new GitHttpFixture
    val request = http.respond(
      s"${http.projectUrl}/repository/compare",
      200,
      Json.obj("compare_timeout" -> false, "diffs" -> Json.arr(diff))
    )

    val result = await(repository(http).compare(main, preview))

    result shouldBe List(CompareDiff(oldPath, newPath, false, true, false, (diff \ "diff").as[String]))
    verify(request).withQueryStringParameters("from" -> main.value, "to" -> preview.value, "straight" -> "true")
    verify(request).get()
  }

  test("HTTP 200 with compare_timeout rejects a nonempty but incomplete file list") {
    val http = new GitHttpFixture
    http.respond(
      s"${http.projectUrl}/repository/compare",
      200,
      Json.obj("compare_timeout" -> true, "diffs" -> Json.arr(diff))
    )

    intercept[IncompleteComparisonException](await(repository(http).compare(main, preview)))
  }

  test("a complete comparison retains changed files whose text diff is empty") {
    val http = new GitHttpFixture
    http.respond(
      s"${http.projectUrl}/repository/compare",
      200,
      Json.obj("compare_timeout" -> false, "diffs" -> Json.arr(diff ++ Json.obj("diff" -> "")))
    )

    await(repository(http).compare(main, preview)) shouldBe
      List(CompareDiff(oldPath, newPath, false, true, false, ""))
  }

  test("a malformed entry rejects the entire comparison instead of returning the valid subset") {
    val http = new GitHttpFixture
    http.respond(
      s"${http.projectUrl}/repository/compare",
      200,
      Json.obj("compare_timeout" -> false, "diffs" -> Json.arr(diff, diff - "new_path"))
    )

    intercept[JsResultException](await(repository(http).compare(main, preview)))
  }

  test("an unsuccessful comparison retains the GitLab status and diagnostic") {
    val http = new GitHttpFixture
    http.respond(s"${http.projectUrl}/repository/compare", 503, Json.obj("message" -> "Comparison unavailable"))

    val error = intercept[GitApiException](await(repository(http).compare(main, preview)))

    error.statusCode shouldBe 503
    error.getMessage should include("Comparison unavailable")
  }

  test("MR changes for the checked SHA use new_path, including renames and deleted files") {
    val http    = new GitHttpFixture
    val deleted = diff ++ Json.obj("old_path" -> oldPath, "new_path" -> oldPath, "deleted_file" -> true)
    val request = http.respond(s"${http.projectUrl}/merge_requests/17/changes", 200, changes(Json.arr(diff, deleted)))

    val result = await(mergeRequests(http).getChanges(mr, Some(preview)))

    result.map(_.path.value) shouldBe List(newPath, oldPath)
    verify(request).get()
  }

  test("MR changes reject overflow even when the response includes valid files and the expected SHA") {
    val http = new GitHttpFixture
    http.respond(s"${http.projectUrl}/merge_requests/17/changes", 200, changes(overflow = true))

    intercept[IllegalStateException](await(mergeRequests(http).getChanges(mr, Some(preview))))
  }

  test("MR changes reject a different head SHA instead of checking files from another revision") {
    val http = new GitHttpFixture
    http.respond(s"${http.projectUrl}/merge_requests/17/changes", 200, changes(sha = main.value))

    val error = intercept[IllegalArgumentException](await(mergeRequests(http).getChanges(mr, Some(preview))))

    error.getMessage should include(main.value)
    error.getMessage should include(preview.value)
  }

  test("a missing head SHA cannot satisfy a pinned MR check") {
    val http = new GitHttpFixture
    http.respond(s"${http.projectUrl}/merge_requests/17/changes", 200, changes() - "diff_refs")

    intercept[JsResultException](await(mergeRequests(http).getChanges(mr, Some(preview))))
  }

  test("an unpinned draft check does not require diff_refs") {
    val http = new GitHttpFixture
    http.respond(s"${http.projectUrl}/merge_requests/17/changes", 200, changes() - "diff_refs")

    await(mergeRequests(http).getChanges(mr)).map(_.path.value) shouldBe List(newPath)
  }

  test("an invalid MR diff rejects the entire list rather than dropping the invalid entry") {
    val http = new GitHttpFixture
    http.respond(s"${http.projectUrl}/merge_requests/17/changes", 200, changes(Json.arr(diff, diff - "new_path")))

    intercept[JsResultException](await(mergeRequests(http).getChanges(mr, Some(preview))))
  }

  for pinned <- List(false, true) do
    test(s"merge sends the SHA only when supplied (pinned=$pinned)") {
      val http    = new GitHttpFixture
      val request = http.respond(s"${http.projectUrl}/merge_requests/17/merge", 200, Json.obj())

      await(mergeRequests(http).merge(mr, if pinned then Some(preview) else None)) shouldBe MergeRequestStatus.Merged

      val expected = Seq("squash" -> "true", "should_remove_source_branch" -> "true") ++
        (if pinned then Seq("sha" -> preview.value) else Nil)
      verify(request).withQueryStringParameters(expected*)
      verify(request).put(EmptyBody)
    }

  test("a GitLab SHA conflict remains a failure and is not retried without the guard") {
    val http    = new GitHttpFixture
    val request = http.respond(
      s"${http.projectUrl}/merge_requests/17/merge",
      409,
      Json.obj("message" -> "SHA does not match HEAD of source branch")
    )

    val error = intercept[GitApiException](await(mergeRequests(http).merge(mr, Some(preview))))

    error.statusCode shouldBe 409
    error.getMessage should include("SHA does not match")
    verify(request).put(EmptyBody)
    verify(http.client).url(s"${http.projectUrl}/merge_requests/17/merge")
    verifyNoMoreInteractions(http.client)
  }

  test("creating the SBB branch sends start_sha and exact create, update and delete actions") {
    val http    = new GitHttpFixture
    val request = http.respond(s"${http.projectUrl}/repository/commits", 201, Json.obj("id" -> preview.value))
    val files   = mock[GitFileService]
    val service = new GitCommitService(
      http.client,
      files,
      http.config,
      GitCommitConfig("fallback@example.com"),
      summon[GitRepositoryConfig],
      summon[ExecutionContext]
    )
    val author  = fakeIdentities.collectFirst { case p: Identity.Person => p }.get
    val actions = Seq(
      GitCommitAction(GitCommitActionType.Create, GitFilePath(newPath), "new\nmodule"),
      GitCommitAction(GitCommitActionType.Update, GitFilePath(oldPath), "updated module"),
      GitCommitAction(GitCommitActionType.Delete, GitFilePath("modules/deleted.md"), "must not be sent")
    )

    await(service.commitToNewBranch(Branch("sbb"), main, author, "Publication", actions)) shouldBe preview

    val body = ArgumentCaptor.forClass(classOf[JsValue])
    verify(request).post(body.capture())(using any[BodyWritable[JsValue]])
    body.getValue shouldBe Json.obj(
      "branch"         -> "sbb",
      "start_sha"      -> main.value,
      "commit_message" -> "Publication",
      "author_email"   -> author.email.get,
      "author_name"    -> author.fullName,
      "actions"        -> Json.arr(
        Json.obj("action" -> "create", "file_path" -> newPath, "content" -> "new\nmodule"),
        Json.obj("action" -> "update", "file_path" -> oldPath, "content" -> "updated module"),
        Json.obj("action" -> "delete", "file_path" -> "modules/deleted.md")
      )
    )
    verifyNoInteractions(files)
  }
}
