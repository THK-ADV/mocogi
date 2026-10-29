package service

import java.util.concurrent.TimeoutException
import java.util.UUID

import scala.concurrent.duration.*
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise

import git.*
import git.api.*
import helper.FakeIdentities
import models.core.Identity
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import parsing.types.Module
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSRequest
import play.api.libs.ws.WSResponse
import service.pipeline.MetadataPipeline
import service.pipeline.PipelineError
import service.pipeline.Print
import settings.SecretString

final class SoftBigBangServiceSpec extends AnyFunSuite with Matchers with MockitoSugar with FakeIdentities {
  import SoftBigBangService.*

  private given ExecutionContext    = ExecutionContext.parasitic
  private given GitRepositoryConfig =
    GitRepositoryConfig(Branch("main"), Branch("preview"), "content/modules", "catalogs", "module-companions")

  private val moduleA        = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private val moduleB        = UUID.fromString("00000000-0000-0000-0000-000000000002")
  private val moduleC        = UUID.fromString("00000000-0000-0000-0000-000000000003")
  private val unrelated      = UUID.fromString("00000000-0000-0000-0000-000000000004")
  private val main           = CommitId("1" * 40)
  private val preview        = CommitId("2" * 40)
  private val currentMain    = CommitId("3" * 40)
  private val currentPreview = CommitId("4" * 40)
  private val committed      = CommitId("5" * 40)
  private val title          = "Soft Big Bang WiSe 26/27 Teil 1"
  private val author         = fakeIdentities.collectFirst { case p: Identity.Person => p }.get
  private val contentA       = "reviewed module A"
  private val defaultActions = Seq(GitCommitAction(GitCommitActionType.Update, GitFilePath(moduleA), contentA))
  private val mrUrl          = "https://git.example/modules/-/merge_requests/17"

  private def await[A](result: Future[A]): A = Await.result(result, 5.seconds)

  private def modified(id: UUID): CompareDiff =
    CompareDiff(GitFilePath(id).value, GitFilePath(id).value, false, false, false, "@@ changed @@")

  private def fileUrl(id: UUID): String =
    s"https://git.example/api/v4/projects/42/repository/files/content%2Fmodules%2F$id.md/raw?ref=${preview.value}"

  private class Fixture {
    val branches   = mock[GitBranchService]
    val repository = mock[GitRepositoryService]
    val downloads  = mock[WSClient]
    val files      = new GitFileService(
      downloads,
      GitApiConfig(SecretString.unsafe("test-token"), "https://git.example/api/v4", 42),
      summon[GitRepositoryConfig],
      summon[ExecutionContext]
    )
    val commits       = mock[GitCommitService]
    val mergeRequests = mock[GitMergeRequestService]
    val pipeline      = mock[MetadataPipeline]

    when(branches.head(branch)).thenReturn(Future.failed(new GitApiException(404, "Not found")))
    when(branches.head(Branch("main"))).thenReturn(Future.successful(main))
    when(branches.head(Branch("preview"))).thenReturn(Future.successful(preview))
    when(repository.compare(main, preview)).thenReturn(Future.successful(List(modified(moduleA))))
    val downloadA = stubDownload(moduleA, contentA)
    when(pipeline.publicationCheck(any[Seq[Print]], any[Set[UUID]]))
      .thenReturn(Future.successful(Right(Nil)))
    stubWrites()

    val service = new SoftBigBangService(
      branches,
      repository,
      files,
      commits,
      mergeRequests,
      mock[ModuleService],
      pipeline,
      GitWebConfig("https://git.example/modules"),
      summon[GitRepositoryConfig],
      summon[ExecutionContext]
    )

    def create(selected: Set[UUID] = Set(moduleA)): Future[String] =
      service.create(main, preview, selected, title, author)

    def stubDownload(id: UUID, content: String): WSRequest = {
      val request  = mock[WSRequest](RETURNS_SELF)
      val response = mock[WSResponse]
      when(response.status).thenReturn(200)
      when(response.bodyAsBytes).thenReturn(ByteString(content))
      when(response.header("X-Gitlab-Last-Commit-Id")).thenReturn(Some(preview.value))
      when(request.get()).thenReturn(Future.successful(response))
      when(downloads.url(fileUrl(id))).thenReturn(request)
      request
    }

    def stubWrites(actions: Seq[GitCommitAction] = defaultActions, base: CommitId = main): Unit = {
      when(commits.commitToNewBranch(branch, base, author, title, actions)).thenReturn(Future.successful(committed))
      when(mergeRequests.create(branch, Branch("main"), title, description(actions), false, List("sbb")))
        .thenReturn(Future.successful((MergeRequestId(17), MergeRequestStatus.Open)))
    }

    def verifyWrites(actions: Seq[GitCommitAction] = defaultActions, base: CommitId = main): Unit = {
      verify(commits).commitToNewBranch(branch, base, author, title, actions)
      verify(mergeRequests).create(branch, Branch("main"), title, description(actions), false, List("sbb"))
      verifyNoMoreInteractions(commits, mergeRequests)
      verify(branches, never()).deleteBranch(branch)
    }

    def verifyNoWrites(): Unit = {
      verifyNoInteractions(commits, mergeRequests)
      verify(branches, never()).deleteBranch(branch)
    }
  }

  private def description(actions: Seq[GitCommitAction]): String =
    actions.map(action => s"- ${action.filePath}").mkString("\n")

  test("unchanged revisions publish the reviewed file exactly once without redundant comparisons") {
    val f = new Fixture

    await(f.create()) shouldBe mrUrl

    f.verifyWrites()
    verify(f.repository).compare(main, preview)
    verifyNoMoreInteractions(f.repository)
    verify(f.downloads).url(fileUrl(moduleA))
    verifyNoMoreInteractions(f.downloads)
    verify(f.pipeline).publicationCheck(Seq(Print(contentA)), Set.empty)
  }

  for (name, before, after) <- Seq(("main", main, currentMain), ("preview", preview, currentPreview)) do
    test(s"a selected module changed on $name rejects the selection before reading or writing files") {
      val f = new Fixture
      when(f.branches.head(Branch(name))).thenReturn(Future.successful(after))
      when(f.repository.compare(before, after))
        .thenReturn(Future.successful(List(modified(moduleA), modified(unrelated))))

      val error = intercept[ModulesChangedException](await(f.create()))

      error.modules shouldBe Set(moduleA)
      f.verifyNoWrites()
      verifyNoInteractions(f.downloads, f.pipeline)
    }

  test("changes on both branches report exactly the affected selected modules, without duplicates") {
    val f = new Fixture
    when(f.branches.head(Branch("main"))).thenReturn(Future.successful(currentMain))
    when(f.branches.head(Branch("preview"))).thenReturn(Future.successful(currentPreview))
    when(f.repository.compare(main, currentMain))
      .thenReturn(Future.successful(List(modified(moduleA), modified(unrelated))))
    when(f.repository.compare(preview, currentPreview))
      .thenReturn(Future.successful(List(modified(moduleA), modified(moduleB))))

    val error = intercept[ModulesChangedException](await(f.create(Set(moduleA, moduleB, moduleC))))

    error.modules shouldBe Set(moduleA, moduleB)
    f.verifyNoWrites()
    verifyNoInteractions(f.downloads, f.pipeline)
  }

  for selected <- List(moduleA, moduleB) do
    test(s"a detected rename invalidates the selection on either side ($selected)") {
      val f      = new Fixture
      val rename = modified(moduleA).copy(newPath = GitFilePath(moduleB).value, renamedFile = true)
      when(f.branches.head(Branch("preview"))).thenReturn(Future.successful(currentPreview))
      when(f.repository.compare(preview, currentPreview)).thenReturn(Future.successful(List(rename)))

      intercept[ModulesChangedException](await(f.create(Set(selected)))).modules shouldBe Set(selected)

      f.verifyNoWrites()
      verifyNoInteractions(f.downloads, f.pipeline)
    }

  test("unrelated concurrent changes are allowed; only selected module files from the reviewed preview are committed") {
    val f       = new Fixture
    val outside =
      List(s"module-companions/$moduleA.md", s"content/modules-extra/$moduleA.md", "content/modules/README.md")
        .map(path => modified(moduleA).copy(oldPath = path, newPath = path))
    when(f.branches.head(Branch("main"))).thenReturn(Future.successful(currentMain))
    when(f.branches.head(Branch("preview"))).thenReturn(Future.successful(currentPreview))
    when(f.repository.compare(main, currentMain)).thenReturn(Future.successful(modified(unrelated) :: outside))
    when(f.repository.compare(preview, currentPreview)).thenReturn(Future.successful(modified(unrelated) :: outside))
    when(f.repository.compare(currentMain, preview)).thenReturn(
      Future.successful(
        List(
          modified(moduleA),
          modified(moduleB).copy(newFile = true),
          modified(moduleC).copy(deletedFile = true),
          modified(unrelated)
        ) ++ outside
      )
    )
    f.stubDownload(moduleB, "reviewed module B")
    val actions = Seq(
      GitCommitAction(GitCommitActionType.Update, GitFilePath(moduleA), contentA),
      GitCommitAction(GitCommitActionType.Create, GitFilePath(moduleB), "reviewed module B"),
      GitCommitAction(GitCommitActionType.Delete, GitFilePath(moduleC), "")
    )
    f.stubWrites(actions, currentMain)

    await(f.create(Set(moduleA, moduleB, moduleC))) shouldBe mrUrl

    f.verifyWrites(actions, currentMain)
    verify(f.downloads).url(fileUrl(moduleA))
    verify(f.downloads).url(fileUrl(moduleB))
    verifyNoMoreInteractions(f.downloads)
    verify(f.pipeline).publicationCheck(Seq(Print(contentA), Print("reviewed module B")), Set(moduleC))
  }

  test("a rename between modules becomes a deletion and an addition at their canonical paths") {
    val f = new Fixture
    when(f.repository.compare(main, preview))
      .thenReturn(
        Future.successful(List(modified(moduleA).copy(newPath = GitFilePath(moduleB).value, renamedFile = true)))
      )
    f.stubDownload(moduleB, "renamed module B")
    val actions = Seq(
      GitCommitAction(GitCommitActionType.Delete, GitFilePath(moduleA), ""),
      GitCommitAction(GitCommitActionType.Create, GitFilePath(moduleB), "renamed module B")
    )
    f.stubWrites(actions)

    await(f.create(Set(moduleA, moduleB))) shouldBe mrUrl

    f.verifyWrites(actions)
    verify(f.downloads).url(fileUrl(moduleB))
    verifyNoMoreInteractions(f.downloads)
    verify(f.pipeline).publicationCheck(Seq(Print("renamed module B")), Set(moduleA))
  }

  for entering <- List(false, true) do
    test(s"a rename crossing the modules folder touches only the canonical module path (entering=$entering)") {
      val f         = new Fixture
      val canonical = GitFilePath(moduleA).value
      val outside   = s"archive/$moduleA.md"
      val diff      =
        if entering then modified(moduleA).copy(oldPath = outside, renamedFile = true)
        else modified(moduleA).copy(newPath = outside, renamedFile = true)
      when(f.repository.compare(main, preview)).thenReturn(Future.successful(List(diff)))
      val actions = Seq(
        GitCommitAction(
          if entering then GitCommitActionType.Create else GitCommitActionType.Delete,
          GitFilePath(canonical),
          if entering then contentA else ""
        )
      )
      f.stubWrites(actions)

      await(f.create()) shouldBe mrUrl

      f.verifyWrites(actions)
      if entering then {
        verify(f.downloads).url(fileUrl(moduleA))
        verifyNoMoreInteractions(f.downloads)
        verify(f.pipeline).publicationCheck(Seq(Print(contentA)), Set.empty)
      } else {
        verifyNoInteractions(f.downloads)
        verify(f.pipeline).publicationCheck(Seq.empty, Set(moduleA))
      }
    }

  test("an existing SBB rejects publication without touching it") {
    val f = new Fixture
    when(f.branches.head(branch)).thenReturn(Future.successful(committed))

    intercept[PendingException](await(f.create()))

    f.verifyNoWrites()
    verifyNoInteractions(f.repository, f.downloads, f.pipeline)
  }

  test("a failed branch lookup is not mistaken for an absent SBB") {
    val f       = new Fixture
    val failure = new GitApiException(503, "GitLab unavailable")
    when(f.branches.head(branch)).thenReturn(Future.failed(failure))

    (intercept[GitApiException](await(f.create())) should be).theSameInstanceAs(failure)

    f.verifyNoWrites()
    verifyNoInteractions(f.repository, f.downloads, f.pipeline)
  }

  test("an incomplete comparison fails closed without publishing any files") {
    val f       = new Fixture
    val failure = new IncompleteComparisonException
    when(f.repository.compare(main, preview)).thenReturn(Future.failed(failure))

    (intercept[IncompleteComparisonException](await(f.create())) should be).theSameInstanceAs(failure)

    f.verifyNoWrites()
    verifyNoInteractions(f.downloads, f.pipeline)
  }

  test("a selection containing a module without changes fails instead of silently publishing a subset") {
    val f = new Fixture

    intercept[IllegalArgumentException](await(f.create(Set(moduleA, moduleB))))

    f.verifyNoWrites()
    verifyNoInteractions(f.downloads, f.pipeline)
  }

  test("publication errors preserve all diagnostics and prevent both commit and MR") {
    val f      = new Fixture
    val errors = Seq(
      PipelineError.validator(Seq(s"Missing module $moduleB"), Some(moduleA)),
      PipelineError.validator(Seq("Multiple parents"), Some(moduleB))
    )
    when(f.pipeline.publicationCheck(any[Seq[Print]], any[Set[UUID]]))
      .thenReturn(Future.successful(Left(errors)))

    val error = intercept[InvalidModulesException](await(f.create()))

    errors.foreach(e => error.getMessage should include(e.getMessage))
    f.verifyNoWrites()
  }

  test("no writes start while the publication check is still pending") {
    val f       = new Fixture
    val checked = Promise[Either[Seq[PipelineError], Seq[(Print, Module)]]]()
    when(f.pipeline.publicationCheck(any[Seq[Print]], any[Set[UUID]])).thenReturn(checked.future)

    val result = f.create()

    result.isCompleted shouldBe false
    verify(f.pipeline).publicationCheck(Seq(Print(contentA)), Set.empty)
    f.verifyNoWrites()
    checked.success(Right(Nil))
    await(result) shouldBe mrUrl
    f.verifyWrites()
  }

  test("a failed file download prevents validation and all writes") {
    val f       = new Fixture
    val failure = new GitApiException(500, "Cannot read file")
    when(f.downloadA.get()).thenReturn(Future.failed(failure))

    (intercept[GitApiException](await(f.create())) should be).theSameInstanceAs(failure)

    f.verifyNoWrites()
    verifyNoInteractions(f.pipeline)
  }

  test("a failed publication check propagates the cause without writes") {
    val f       = new Fixture
    val failure = new IllegalStateException("Published module lookup failed")
    when(f.pipeline.publicationCheck(any[Seq[Print]], any[Set[UUID]])).thenReturn(Future.failed(failure))

    (intercept[IllegalStateException](await(f.create())) should be).theSameInstanceAs(failure)

    f.verifyNoWrites()
  }

  test("a failed commit does not create an MR or delete a branch") {
    val f       = new Fixture
    val failure = new GitApiException(400, "Branch already exists")
    when(f.commits.commitToNewBranch(branch, main, author, title, defaultActions)).thenReturn(Future.failed(failure))

    (intercept[GitApiException](await(f.create())) should be).theSameInstanceAs(failure)

    verify(f.commits).commitToNewBranch(branch, main, author, title, defaultActions)
    verifyNoMoreInteractions(f.commits)
    verifyNoInteractions(f.mergeRequests)
    verify(f.branches, never()).deleteBranch(branch)
  }

  test("a lost MR response retains the committed branch because GitLab may already have created the MR") {
    val f       = new Fixture
    val failure = new TimeoutException("MR response lost")
    when(f.mergeRequests.create(branch, Branch("main"), title, description(defaultActions), false, List("sbb")))
      .thenReturn(Future.failed(failure))

    (intercept[TimeoutException](await(f.create())) should be).theSameInstanceAs(failure)

    f.verifyWrites()
  }
}
