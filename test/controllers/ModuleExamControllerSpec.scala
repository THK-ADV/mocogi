package controllers

import java.util.UUID

import scala.concurrent.Future

import auth.Authorization
import auth.CampusId
import auth.Token
import cli.GitCLI
import cli.MarkdownCLI
import database.repo.ModuleExamRepository
import database.repo.PermissionRepository
import helper.FakeApplication
import helper.FakeIdentities
import models.core.Identity
import models.ModuleExams
import org.mockito.ArgumentMatchers.any
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

final class ModuleExamControllerSpec
    extends AnyFunSuite
    with Matchers
    with MockitoSugar
    with GuiceOneAppPerSuite
    with FakeApplication
    with FakeIdentities
    with BeforeAndAfterEach {

  private val repo          = mock[ModuleExamRepository]
  private val authorization = mock[Authorization]
  private val permissions   = mock[PermissionRepository]
  private val modulePerms   = mock[ModuleUpdatePermissionService]
  private val person        = fakeIdentities.collectFirst { case p: Identity.Person => p }.get
  private val token         = Token.UserToken(person.firstname, person.lastname, "ald", "ald@example.com", Set.empty)
  private val admin         = Permissions(Map(PermissionType.Admin -> Set.empty))
  private val moduleId      = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private val url           = s"/moduleExams/$moduleId"

  protected override def bindings: Seq[GuiceableModule] =
    Seq(
      bind[ModuleExamRepository].toInstance(repo),
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
    when(repo.replace(any[UUID], any[Option[List[String]]])).thenReturn(Future.successful(Some(true)))
  }

  private def put(json: JsValue) =
    route(app, FakeRequest(PUT, url).withHeaders(AUTHORIZATION -> "Bearer test").withJsonBody(json)).get

  private def get() = route(app, FakeRequest(GET, url).withHeaders(AUTHORIZATION -> "Bearer test")).get

  test("PUT trims exam names and stores and returns the trimmed list") {
    val response = put(Json.obj("examNames" -> Json.arr("  Klausur ", "\tPraktikum")))

    status(response) shouldBe OK
    contentAsJson(response) shouldBe Json.obj("examNames" -> Json.arr("Klausur", "Praktikum"))
    verify(repo).replace(moduleId, Some(List("Klausur", "Praktikum")))
  }

  test("PUT with null exam names selects the default exam and returns null") {
    val response = put(Json.obj("examNames" -> JsNull))

    status(response) shouldBe OK
    contentAsJson(response) shouldBe Json.obj("examNames" -> JsNull)
    verify(repo).replace(moduleId, None)
  }

  test("PUT without the examNames key is rejected instead of resetting to the default exam") {
    status(put(Json.obj())) shouldBe BAD_REQUEST
    verifyNoInteractions(repo)
  }

  for (label, names) <- Seq(
      "empty list"                  -> Json.arr(),
      "blank name"                  -> Json.arr("Klausur", "   "),
      "line feed in name"           -> Json.arr("Klau\nsur"),
      "carriage return in name"     -> Json.arr("Klau\rsur"),
      "duplicate after trimming"    -> Json.arr("A", " A "),
      "duplicate ignoring case"     -> Json.arr("Klausur", "klausur"),
      "non-string name in the list" -> Json.arr("A", 1)
    )
  do
    test(s"PUT rejects invalid exam names with 400 without touching the repository: $label") {
      status(put(Json.obj("examNames" -> names))) shouldBe BAD_REQUEST
      verifyNoInteractions(repo)
    }

  test("PUT on a child or generic module without own exams returns 400") {
    when(repo.replace(moduleId, None)).thenReturn(Future.successful(Some(false)))

    val response = put(Json.obj("examNames" -> JsNull))

    status(response) shouldBe BAD_REQUEST
    (contentAsJson(response) \ "message").as[String] should include(moduleId.toString)
  }

  test("PUT on an unknown module returns 404") {
    when(repo.replace(moduleId, None)).thenReturn(Future.successful(None))

    status(put(Json.obj("examNames" -> JsNull))) shouldBe NOT_FOUND
  }

  test("GET returns the module title and its exam names, null for the default exam") {
    when(repo.get(moduleId)).thenReturn(Future.successful(Some(("Analysis", ModuleExams(Some(List("Klausur")))))))
    contentAsJson(get()) shouldBe Json.obj("title" -> "Analysis", "examNames" -> Json.arr("Klausur"))

    when(repo.get(moduleId)).thenReturn(Future.successful(Some(("Analysis", ModuleExams(None)))))
    contentAsJson(get()) shouldBe Json.obj("title" -> "Analysis", "examNames" -> JsNull)
  }

  test("GET on an unknown module returns 404") {
    when(repo.get(moduleId)).thenReturn(Future.successful(None))

    status(get()) shouldBe NOT_FOUND
  }

  for (label, granted, author) <- Seq(("granted permission", true, false), ("authorship", false, true)) do
    test(s"PUT allows users without admin rights through $label") {
      when(permissions.all(CampusId("ald"))).thenReturn(Future.successful(Some((person, Permissions(Map.empty)))))
      when(modulePerms.hasPermissionFor(moduleId, CampusId("ald"))).thenReturn(Future.successful(granted))
      when(modulePerms.isAuthorOf(moduleId, person.id)).thenReturn(Future.successful(author))

      status(put(Json.obj("examNames" -> JsNull))) shouldBe OK
      verify(repo).replace(moduleId, None)
    }

  for method <- Seq(GET, PUT) do {
    test(s"$method rejects unauthenticated requests before resolving the user") {
      val response = route(app, FakeRequest(method, url).withJsonBody(Json.obj("examNames" -> JsNull))).get

      status(response) shouldBe UNAUTHORIZED
      verifyNoInteractions(permissions, repo)
    }

    test(s"$method rejects users without permission to edit the module") {
      when(permissions.all(CampusId("ald"))).thenReturn(Future.successful(Some((person, Permissions(Map.empty)))))
      when(modulePerms.hasPermissionFor(moduleId, CampusId("ald"))).thenReturn(Future.successful(false))
      when(modulePerms.isAuthorOf(moduleId, person.id)).thenReturn(Future.successful(false))

      val response = route(
        app,
        FakeRequest(method, url)
          .withHeaders(AUTHORIZATION -> "Bearer test")
          .withJsonBody(Json.obj("examNames" -> JsNull))
      ).get

      status(response) shouldBe FORBIDDEN
      verifyNoInteractions(repo)
    }
  }
}
