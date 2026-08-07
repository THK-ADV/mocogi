package service.moduledetails

import java.time.LocalDateTime
import java.util.UUID

import scala.concurrent.duration.*
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.repo.ModuleDetailRepository
import git.api.GitCommitService
import git.api.GitFileService
import git.Branch
import models.ModuleDraft
import models.ModuleDraftSource
import org.mockito.Mockito.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import parsing.types.Module
import play.api.libs.json.JsNull
import service.pipeline.MetadataPipeline
import service.pipeline.Print
import service.ModuleDraftService

final class ModuleDetailsServiceSpec extends AnyFunSuite with Matchers with MockitoSugar {

  private given ExecutionContext = ExecutionContext.global

  test("persisted drafts bypass Git") {
    val repository       = mock[ModuleDetailRepository]
    val gitFileService   = mock[GitFileService]
    val gitCommitService = mock[GitCommitService]
    val draftService     = mock[ModuleDraftService]
    val pipeline         = mock[MetadataPipeline]
    val module           = mock[Module]
    val details          = mock[ModuleDetails]
    val moduleId         = UUID.randomUUID()
    val print            = Print("draft")
    val lastModified     = LocalDateTime.now()
    val draft            = ModuleDraft(
      moduleId,
      "title",
      "abbreviation",
      "author",
      Branch("draft"),
      ModuleDraftSource.Modified,
      JsNull,
      JsNull,
      print,
      Set.empty,
      Set.empty,
      None,
      None,
      lastModified
    )

    when(draftService.getByModuleOpt(moduleId)).thenReturn(Future.successful(Some(draft)))
    when(pipeline.parseValidate(print)).thenReturn(Future.successful(module))
    when(repository.assemble(module, lastModified)).thenReturn(Future.successful(details))

    val service = ModuleDetailsService(
      repository,
      gitFileService,
      gitCommitService,
      draftService,
      pipeline,
      summon[ExecutionContext]
    )
    clearInvocations(gitFileService)

    Await.result(service.latest(moduleId), 5.seconds) shouldBe Some(details)
    verifyNoInteractions(gitFileService, gitCommitService)
  }

}
