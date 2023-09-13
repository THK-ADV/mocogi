package service

import database._
import git.api.{GitBranchService, GitCommitService}
import helper.FakeApplication
import models._
import models.core.{Status, _}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest._
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import parsing.metadata.VersionScheme
import parsing.types._
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.JsNull
import validator.{Metadata, POs, Prerequisites, Workload}

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Future

final class ModuleDraftServiceSpec
    extends TestSuite
    with GuiceOneAppPerSuite
    with FakeApplication {

  val branchService = mock(classOf[GitBranchService])
  val commitService = mock(classOf[GitCommitService])

  // import play.api.inject.bind
  override def bindings: Seq[GuiceableModule] = Seq(
    play.api.inject
      .bind(classOf[GitBranchService])
      .toInstance(branchService),
    play.api.inject
      .bind(classOf[GitCommitService])
      .toInstance(commitService)
  )

  def fakeDraft() =
    ModuleDraft(
      UUID.randomUUID(),
      User(""),
      Branch(""),
      ModuleDraftSource.Modified,
      JsNull,
      JsNull,
      Print(""),
      None,
      None,
      None,
      LocalDateTime.now
    )

  def fakeProtocol() = // TODO remove prod dependency
    ModuleCompendiumProtocol(
      MetadataProtocol(
        "t",
        "a",
        "module",
        1.0,
        "de",
        1,
        "ss",
        ParsedWorkload(0, 0, 0, 0, 0, 0),
        "active",
        "gm",
        None,
        None,
        List("ado"),
        List("ado"),
        AssessmentMethodsOutput(
          List(AssessmentMethodEntryOutput("written-exam", None, Nil)),
          Nil
        ),
        PrerequisitesOutput(None, None),
        POOutput(List(POMandatoryOutput("inf_mi4", None, List(1), Nil)), Nil),
        Nil,
        Nil,
        Nil
      ),
      fakeContent(),
      fakeContent()
    )

  private def fakeContent() =
    Content("", "", "", "", "")

  def fakeParsedMetadata() =
    ParsedMetadata(
      UUID.randomUUID(),
      "",
      "",
      ModuleType("", "", ""),
      None,
      Left(0.0),
      Language("", "", ""),
      0,
      Season("", "", ""),
      Responsibilities(Nil, Nil),
      AssessmentMethods(Nil, Nil),
      ParsedWorkload(0, 0, 0, 0, 0, 0),
      ParsedPrerequisites(None, None),
      Status("", "", ""),
      Location("", "", ""),
      ParsedPOs(Nil, Nil),
      None,
      Nil,
      Nil,
      Nil
    )

  def fakeMetadata() =
    Metadata(
      UUID.randomUUID(),
      "",
      "",
      ModuleType("", "", ""),
      None,
      ECTS(0.0, Nil),
      Language("", "", ""),
      0,
      Season("", "", ""),
      Responsibilities(Nil, Nil),
      AssessmentMethods(Nil, Nil),
      Workload(0, 0, 0, 0, 0, 0, 0, 0),
      Prerequisites(None, None),
      Status("", "", ""),
      Location("", "", ""),
      POs(Nil, Nil),
      None,
      Nil,
      Nil,
      Nil
    )

  // workaround: https://github.com/playframework/scalatestplus-play/issues/112
  val nestedSuite: AsyncWordSpec = new AsyncWordSpec {
    val service = app.injector.instanceOf(classOf[ModuleDraftService])

    "A Module Draft Service" should {
      "return all drafts from a given user" in {
        withFreshDb().flatMap(_ =>
          service.allByModules(User("alex")).map(xs => assert(xs.isEmpty))
        )
      }

      "create a new draft for a new module" when {
        "everything goes fine" in {
//          when(printer.printer(any())).thenReturn(Printer { case (_) =>
//            Right("ok")
//          })
//          when(parser.parse(any()))
//            .thenReturn(
//              Future.successful(
//                Right((fakeParsedMetadata(), fakeContent(), fakeContent()))
//              )
//            )
//          when(validator.validate(any()))
//            .thenReturn(Future.successful(Right(fakeMetadata())))
          when(branchService.createBranch(any()))
            .thenReturn(Future.successful(Branch("")))
          when(commitService.commit(any(), any(), any(), any(), any()))
            .thenReturn(Future.successful(CommitId("")))
//          when(moduleDraftRepo.create(any()))
//            .thenReturn(Future.successful(fakeDraft()))

          service
            .createNew(fakeProtocol(), User("alex"), VersionScheme(1.0, "s"))
            .map {
              case Left(err) =>
                fail(err)
              case Right(d) =>
                println(d)
                succeed
            }
        }
//        "printer fails" in {
////          when(printer.printer(any()))
////            .thenReturn(Printer[(UUID, ModuleCompendiumProtocol)] { case (_) =>
////              Left(PrintingError("a", "b"))
////            })
//
//          service
//            .createNew(fakeProtocol(), User("alex"), VersionScheme(1.0, "s"))
//            .map {
//              case Left(err) =>
//                assert(err.metadata.nonEmpty)
//                assert(err.getMessage.nonEmpty)
//              case Right(_) => fail()
//            }
//        }
      }
    }
  }

  override def nestedSuites: IndexedSeq[Suite] = Vector(nestedSuite)
}
