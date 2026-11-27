package controllers

import java.util.UUID

import scala.concurrent.Future

import auth.Authorization
import auth.CampusId
import auth.Token
import cli.GitCLI
import cli.MarkdownCLI
import database.repo.ExamRepository
import database.repo.PermissionRepository
import helper.FakeApplication
import helper.FakeIdentities
import models.core.Identity
import org.mockito.Mockito.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import permission.PermissionType
import permission.Permissions
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.*
import play.api.libs.mailer.MailerClient
import play.api.libs.ws.WSClient
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Environment
import play.api.Mode
import security.ClientErrorResponse
import service.ModuleUpdatePermissionService

final class ExamControllerSpec
    extends AnyFunSuite
    with Matchers
    with MockitoSugar
    with GuiceOneAppPerSuite
    with FakeApplication
    with FakeIdentities
    with BeforeAndAfterEach {

  private val repo          = mock[ExamRepository]
  private val authorization = mock[Authorization]
  private val permissions   = mock[PermissionRepository]
  private val modulePerms   = mock[ModuleUpdatePermissionService]
  private val person        = fakeIdentities.collectFirst { case p: Identity.Person => p }.get
  private val token         = Token.UserToken(person.firstname, person.lastname, "ald", "ald@example.com", Set.empty)
  private val admin         = Permissions(Map(PermissionType.Admin -> Set.empty))
  private val moduleId      = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private val url           = s"/modules/$moduleId/exams"

  protected override def bindings: Seq[GuiceableModule] =
    Seq(
      bind[ExamRepository].toInstance(repo),
      bind[Authorization].toInstance(authorization),
      bind[PermissionRepository].toInstance(permissions),
      bind[ModuleUpdatePermissionService].toInstance(modulePerms),
      bind[WSClient].toInstance(mock[WSClient]),
      bind[MailerClient].toInstance(mock[MailerClient]),
      bind[GitCLI].toInstance(mock[GitCLI]),
      bind[MarkdownCLI].toInstance(mock[MarkdownCLI]),
      bind[ClientErrorResponse].toInstance(new ClientErrorResponse(Environment.simple(mode = Mode.Prod)))
    )

  protected override def beforeEach(): Unit = {
    super.beforeEach()
    reset(repo, authorization, permissions, modulePerms)
    when(authorization.authorize(Some("Bearer test"))).thenReturn(Future.successful(token))
    when(authorization.authorize(None)).thenReturn(Future.failed(new IllegalArgumentException("Missing token")))
    when(permissions.all(CampusId("ald"))).thenReturn(Future.successful(Some((person, admin))))
  }

  private def get() = route(app, FakeRequest(GET, url).withHeaders(AUTHORIZATION -> "Bearer test")).get

  test("GET returns the module exams as read") {
    val json = Json.obj("title" -> "Analysis", "hasOwnExams" -> true, "exams" -> Json.arr())
    when(repo.moduleExams(moduleId)).thenReturn(Future.successful(Some(json)))

    contentAsJson(get()) shouldBe json
  }

  test("GET on an unknown module returns 404") {
    when(repo.moduleExams(moduleId)).thenReturn(Future.successful(None))

    status(get()) shouldBe NOT_FOUND
  }

  for (label, granted, author) <- Seq(("granted permission", true, false), ("authorship", false, true)) do
    test(s"GET allows users without admin rights through $label") {
      when(permissions.all(CampusId("ald"))).thenReturn(Future.successful(Some((person, Permissions(Map.empty)))))
      when(modulePerms.hasPermissionFor(moduleId, CampusId("ald"))).thenReturn(Future.successful(granted))
      when(modulePerms.isAuthorOf(moduleId, person.id)).thenReturn(Future.successful(author))
      when(repo.moduleExams(moduleId)).thenReturn(Future.successful(Some(Json.obj())))

      status(get()) shouldBe OK
    }

  for method <- Seq(GET) do {
    test(s"$method rejects unauthenticated requests before resolving the user") {
      val response = route(app, FakeRequest(method, url)).get

      status(response) shouldBe UNAUTHORIZED
      verifyNoInteractions(permissions, repo)
    }

    test(s"$method rejects users without permission to edit the module") {
      when(permissions.all(CampusId("ald"))).thenReturn(Future.successful(Some((person, Permissions(Map.empty)))))
      when(modulePerms.hasPermissionFor(moduleId, CampusId("ald"))).thenReturn(Future.successful(false))
      when(modulePerms.isAuthorOf(moduleId, person.id)).thenReturn(Future.successful(false))

      val response = route(
        app,
        FakeRequest(method, url).withHeaders(AUTHORIZATION -> "Bearer test")
      ).get

      status(response) shouldBe FORBIDDEN
      verifyNoInteractions(repo)
    }
  }
}
