package webhook

import java.util.UUID

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise

import com.typesafe.config.ConfigFactory
import database.repo.ModuleDraftRepository
import database.repo.ModuleRepository
import database.repo.ModuleReviewRepository
import git.*
import git.api.*
import helper.GitHttpFixture
import logging.CorrelationId
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.Props
import org.apache.pekko.testkit.ExplicitlyTriggeredScheduler
import org.apache.pekko.testkit.TestActorRef
import org.apache.pekko.testkit.TestKit
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.mockito.MockitoSugar
import parsing.types.Module
import play.api.libs.json.*
import service.notification.ReviewRejectionNotifier
import service.pipeline.MetadataPipeline
import service.pipeline.PipelineError
import service.pipeline.Print
import service.ModuleCreationService

final class MergeEventHandlerSpec extends AnyFunSuite with Matchers with MockitoSugar with BeforeAndAfterAll {
  private given ExecutionContext = ExecutionContext.parasitic
  private given ActorSystem      =
    ActorSystem(
      "merge-event-handler-spec",
      ConfigFactory.parseString(
        """pekko.scheduler.implementation = "org.apache.pekko.testkit.ExplicitlyTriggeredScheduler"
          |pekko.loglevel = "WARNING"
          |""".stripMargin
      )
    )
  private val clock                 = summon[ActorSystem].scheduler.asInstanceOf[ExplicitlyTriggeredScheduler]
  private given GitRepositoryConfig =
    GitRepositoryConfig(Branch("main"), Branch("preview"), "modules", "catalogs", "companions")

  private val mr             = MergeRequestId(17)
  private val sbb            = Branch("sbb")
  private val checkedSha     = CommitId("1" * 40)
  private val newerSha       = CommitId("2" * 40)
  private val moduleId       = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private val deletedId      = UUID.fromString("00000000-0000-0000-0000-000000000002")
  private val successComment = "✅ successfully type checked all modules"
  private val errors         = Seq(
    PipelineError.validator(Seq("Missing prerequisite"), Some(moduleId)),
    PipelineError.validator(Seq("Multiple parents"), Some(deletedId))
  )
  private val failureComment = "❌ failed to type check modules.\n\n" + errors.map(_.getMessage).mkString("\n\n")
  private val ready          = Json.obj("detailed_merge_status" -> "mergeable", "merge_status" -> "can_be_merged")
  private val checking       = Json.obj("detailed_merge_status" -> "checking", "merge_status" -> "checking")

  protected override def afterAll(): Unit = {
    try TestKit.shutdownActorSystem(summon[ActorSystem])
    finally super.afterAll()
  }

  private def event(
      source: String = "sbb",
      target: String = "main",
      action: String = "open",
      labels: Seq[String] = Seq("sbb")
  ): HandleEvent =
    HandleEvent(
      Json.obj(
        "object_attributes" -> Json.obj(
          "iid"           -> mr.value,
          "source_branch" -> source,
          "target_branch" -> target,
          "action"        -> action,
          "labels"        -> labels.map(label => Json.obj("title" -> label))
        )
      ),
      CorrelationId.random()
    )

  private class Fixture {
    val http        = new GitHttpFixture
    val branches    = mock[GitBranchService]
    val mrs         = mock[GitMergeRequestService]
    val drafts      = mock[ModuleDraftRepository]
    val reviews     = mock[ModuleReviewRepository]
    val creation    = mock[ModuleCreationService]
    val pipeline    = mock[MetadataPipeline]
    val files       = new GitFileService(http.client, http.config, summon[GitRepositoryConfig], summon[ExecutionContext])
    val fileUrl     = s"${http.projectUrl}/repository/files/modules%2F$moduleId.md/raw?ref=${checkedSha.value}"
    val deletedUrl  = s"${http.projectUrl}/repository/files/modules%2F$deletedId.md/raw?ref=${checkedSha.value}"
    val fileRequest = http.file(fileUrl, 200, "checked module content")
    http.file(deletedUrl, 404, "")

    when(branches.head(sbb)).thenReturn(Future.successful(checkedSha))
    when(branches.deleteBranch(sbb)).thenReturn(Future.unit)
    when(mrs.getChanges(mr, Some(checkedSha))).thenReturn(
      Future.successful(
        List(
          Diff("", GitFilePath(moduleId)),
          Diff("", GitFilePath(deletedId)),
          Diff("", GitFilePath(s"companions/$moduleId.md"))
        )
      )
    )
    when(pipeline.publicationCheck(Seq(Print("checked module content")), Set(deletedId)))
      .thenReturn(Future.successful(Right(Nil)))
    when(mrs.comment(mr, successComment)).thenReturn(Future.unit)
    when(mrs.comment(mr, failureComment)).thenReturn(Future.unit)
    when(mrs.get(mr)).thenReturn(Future.successful((200, ready)))
    when(mrs.merge(mr, Some(checkedSha))).thenReturn(Future.successful(MergeRequestStatus.Merged))
    when(mrs.merge(mr, None)).thenReturn(Future.successful(MergeRequestStatus.Merged))
    when(mrs.close(mr)).thenReturn(Future.successful(MergeRequestStatus.Closed))
    when(drafts.updateMergeRequestStatus(moduleId, MergeRequestStatus.Merged)).thenReturn(Future.unit)

    val actor = TestActorRef[MergeEventHandler](
      Props(
        new MergeEventHandler(
          summon[GitRepositoryConfig],
          GitReviewConfig("auto-approved", "review-required", "review-bypass"),
          reviews,
          drafts,
          creation,
          mrs,
          branches,
          mock[ModuleRepository],
          mock[GitCommitService],
          files,
          mock[ReviewRejectionNotifier],
          pipeline,
          summon[ExecutionContext]
        )
      )
    )

    def rejectValidation(): Unit =
      when(pipeline.publicationCheck(Seq(Print("checked module content")), Set(deletedId)))
        .thenReturn(Future.successful(Left(errors)))

    def verifyNoMerge(): Unit = verify(mrs, never()).merge(MergeRequestId(anyInt()), any[Option[CommitId]])

    def verifyRetained(): Unit = {
      verify(mrs, never()).close(mr)
      verify(branches, never()).deleteBranch(sbb)
      verifyNoInteractions(drafts, reviews, creation)
    }
  }

  test("SBB pins changes, downloads and merge to the checked SHA, including across a retry and a concurrent push") {
    val f = new Fixture
    when(f.mrs.get(mr)).thenReturn(Future.successful((200, checking)), Future.successful((200, ready)))

    f.actor ! event()
    verify(f.mrs).getChanges(mr, Some(checkedSha))
    verify(f.http.client).url(f.fileUrl)
    verify(f.http.client).url(f.deletedUrl)
    verifyNoMoreInteractions(f.http.client)
    verify(f.pipeline).publicationCheck(Seq(Print("checked module content")), Set(deletedId))
    verifyNoMoreInteractions(f.pipeline)
    when(f.branches.head(sbb)).thenReturn(Future.successful(newerSha))

    clock.timePasses(4.seconds)
    verify(f.mrs).get(mr)
    f.verifyNoMerge()
    clock.timePasses(5.seconds)

    verify(f.mrs, times(2)).get(mr)
    verify(f.mrs).comment(mr, successComment)
    verify(f.mrs).merge(mr, Some(checkedSha))
    verifyNoMoreInteractions(f.mrs)
    verify(f.branches).head(sbb)
    f.verifyRetained()
  }

  test("pending validation cannot schedule a merge, even after advancing the clock") {
    val f      = new Fixture
    val result = Promise[Either[Seq[PipelineError], Seq[(Print, Module)]]]()
    when(f.pipeline.publicationCheck(Seq(Print("checked module content")), Set(deletedId))).thenReturn(result.future)

    f.actor ! event()
    clock.timePasses(1.hour)

    verify(f.mrs, never()).get(mr)
    f.verifyNoMerge()
    f.verifyRetained()
    result.success(Right(Nil))
    clock.timePasses(4.seconds)
    verify(f.mrs).merge(mr, Some(checkedSha))
  }

  test("invalid modules are reported and the branch is deleted only after the MR has closed") {
    val f = new Fixture
    f.rejectValidation()
    val closed = Promise[MergeRequestStatus]()
    when(f.mrs.close(mr)).thenReturn(closed.future)

    f.actor ! event()
    clock.timePasses(1.hour)

    verify(f.mrs).comment(mr, failureComment)
    verify(f.mrs).close(mr)
    verify(f.branches, never()).deleteBranch(sbb)
    f.verifyNoMerge()
    closed.success(MergeRequestStatus.Closed)
    val order = org.mockito.Mockito.inOrder(f.mrs, f.branches)
    order.verify(f.mrs).comment(mr, failureComment)
    order.verify(f.mrs).close(mr)
    order.verify(f.branches).deleteBranch(sbb)
    verifyNoInteractions(f.drafts, f.reviews, f.creation)
  }

  test("a failed MR close retains the branch and never schedules a merge") {
    val f = new Fixture
    f.rejectValidation()
    when(f.mrs.close(mr)).thenReturn(Future.failed(new GitApiException(503, "Close failed")))

    f.actor ! event()
    clock.timePasses(1.hour)

    verify(f.mrs).close(mr)
    verify(f.branches, never()).deleteBranch(sbb)
    f.verifyNoMerge()
  }

  test("a failed validation comment leaves the MR and branch available for inspection") {
    val f = new Fixture
    f.rejectValidation()
    when(f.mrs.comment(mr, failureComment)).thenReturn(Future.failed(new GitApiException(503, "Comment failed")))

    f.actor ! event()
    clock.timePasses(1.hour)

    verify(f.mrs).comment(mr, failureComment)
    f.verifyNoMerge()
    f.verifyRetained()
  }

  for failure <- Seq(new GitApiException(503, "Changes unavailable"), new IllegalArgumentException("Head SHA changed"))
  do
    test(s"failed change retrieval neither merges nor cleans up: ${failure.getMessage}") {
      val f = new Fixture
      when(f.mrs.getChanges(mr, Some(checkedSha))).thenReturn(Future.failed(failure))

      f.actor ! event()
      clock.timePasses(1.hour)

      verifyNoInteractions(f.http.client, f.pipeline)
      f.verifyNoMerge()
      f.verifyRetained()
    }

  test("a failed file download neither validates nor merges nor deletes") {
    val f = new Fixture
    when(f.fileRequest.get()).thenReturn(Future.failed(new GitApiException(503, "Download failed")))

    f.actor ! event()
    clock.timePasses(1.hour)

    verifyNoInteractions(f.pipeline)
    f.verifyNoMerge()
    f.verifyRetained()
  }

  test("a technical pipeline failure does not trigger the invalid-module cleanup") {
    val f = new Fixture
    when(f.pipeline.publicationCheck(Seq(Print("checked module content")), Set(deletedId)))
      .thenReturn(Future.failed(new IllegalStateException("Database unavailable")))

    f.actor ! event()
    clock.timePasses(1.hour)

    f.verifyNoMerge()
    f.verifyRetained()
  }

  test("a failed mergeability lookup does not merge or delete the checked SBB") {
    val f = new Fixture
    when(f.mrs.get(mr)).thenReturn(Future.failed(new GitApiException(503, "Status unavailable")))

    f.actor ! event()
    clock.timePasses(4.seconds)

    verify(f.mrs).get(mr)
    f.verifyNoMerge()
    f.verifyRetained()
  }

  test("a SHA conflict during merge retains the MR and never retries without the checked SHA") {
    val f = new Fixture
    when(f.mrs.merge(mr, Some(checkedSha))).thenReturn(Future.failed(new GitApiException(409, "Source changed")))

    f.actor ! event()
    clock.timePasses(4.seconds)
    clock.timePasses(1.hour)

    verify(f.mrs).merge(mr, Some(checkedSha))
    verify(f.mrs, never()).merge(mr, None)
    f.verifyRetained()
  }

  test("a persistently unmergeable SBB stops after ten status checks without merging or deleting") {
    val f = new Fixture
    when(f.mrs.get(mr)).thenReturn(Future.successful((200, checking)))

    f.actor ! event()
    for _ <- 1 to 12 do clock.timePasses(1.hour)

    verify(f.mrs, times(10)).get(mr)
    f.verifyNoMerge()
    f.verifyRetained()
  }

  for (source, target, action, labels) <- Seq(
      ("sbb", "main", "open", Seq("other")),
      ("sbb", "preview", "open", Seq("sbb")),
      ("another-branch", "main", "open", Seq("sbb")),
      ("sbb", "main", "approved", Seq("sbb"))
    )
  do
    test(s"unrelated events do not enter the SBB workflow: $source->$target $action $labels") {
      val f = new Fixture

      f.actor ! event(source, target, action, labels)
      clock.timePasses(1.hour)

      verifyNoInteractions(f.branches, f.mrs, f.http.client, f.pipeline, f.drafts, f.reviews, f.creation)
    }

  for (action, label) <- Seq(("open", "auto-approved"), ("approved", "review-required")) do
    test(s"ordinary draft MRs still merge without a SHA and update their draft status ($action)") {
      val f      = new Fixture
      val merged = Promise[MergeRequestStatus]()
      when(f.mrs.merge(mr, None)).thenReturn(merged.future)

      f.actor ! event(moduleId.toString, "preview", action, Seq(label))
      clock.timePasses(4.seconds)

      verify(f.mrs).merge(mr, None)
      verifyNoInteractions(f.drafts)
      merged.success(MergeRequestStatus.Merged)
      verify(f.drafts).updateMergeRequestStatus(moduleId, MergeRequestStatus.Merged)
      verifyNoMoreInteractions(f.drafts)
      verifyNoInteractions(f.branches, f.http.client, f.pipeline, f.reviews, f.creation)
    }

  test("a failed ordinary draft merge does not mark its draft as merged") {
    val f = new Fixture
    when(f.mrs.merge(mr, None)).thenReturn(Future.failed(new GitApiException(409, "Merge conflict")))

    f.actor ! event(moduleId.toString, "preview", "open", Seq("auto-approved"))
    clock.timePasses(4.seconds)

    verify(f.mrs).merge(mr, None)
    verifyNoInteractions(f.drafts)
  }
}
