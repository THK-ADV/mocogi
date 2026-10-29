package controllers

import java.util.UUID

import scala.concurrent.Future

import auth.Authorization
import auth.CampusId
import auth.Token
import cli.GitCLI
import cli.MarkdownCLI
import database.repo.PermissionRepository
import git.api.IncompleteComparisonException
import git.CommitId
import git.CompareDiff
import helper.FakeApplication
import helper.FakeIdentities
import models.core.Identity
import models.ModuleCore
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
import service.SoftBigBangService
import service.SoftBigBangService.*

final class SoftBigBangControllerSpec
    extends AnyFunSuite
    with Matchers
    with MockitoSugar
    with GuiceOneAppPerSuite
    with FakeApplication
    with FakeIdentities
    with BeforeAndAfterEach {

  private val service       = mock[SoftBigBangService]
  private val authorization = mock[Authorization]
  private val permissions   = mock[PermissionRepository]
  private val http          = mock[WSClient]
  private val mailer        = mock[MailerClient]
  private val gitCli        = mock[GitCLI]
  private val markdown      = mock[MarkdownCLI]
  private val person        = fakeIdentities.collectFirst { case p: Identity.Person => p }.get
  private val token         = Token.UserToken(person.firstname, person.lastname, "ald", "ald@example.com", Set("admin"))
  private val admin         = Permissions(Map(PermissionType.Admin -> Set.empty))
  private val main          = CommitId("1" * 40)
  private val preview       = CommitId("2" * 40)
  private val moduleId      = UUID.fromString("00000000-0000-0000-0000-000000000001")
  private val otherId       = UUID.fromString("00000000-0000-0000-0000-000000000002")
  private val title         = "Soft Big Bang WiSe 26/27 Teil 1"
  private val url           = "/admin/softBigBang"
  private val mrUrl         = "https://git.example/modules/-/merge_requests/17"
  private val body          = Json.obj(
    "mainSha"    -> main.value,
    "previewSha" -> preview.value,
    "modules"    -> Json.arr(moduleId.toString),
    "title"      -> title
  )

  protected override def bindings: Seq[GuiceableModule] =
    Seq(
      bind[SoftBigBangService].toInstance(service),
      bind[Authorization].toInstance(authorization),
      bind[PermissionRepository].toInstance(permissions),
      bind[WSClient].toInstance(http),
      bind[MailerClient].toInstance(mailer),
      bind[GitCLI].toInstance(gitCli),
      bind[MarkdownCLI].toInstance(markdown),
      bind[ClientErrorResponse].toInstance(new ClientErrorResponse(Environment.simple(mode = Mode.Prod)))
    )

  protected override def beforeEach(): Unit = {
    super.beforeEach()
    reset(service, authorization, permissions)
    when(authorization.authorize(Some("Bearer test"))).thenReturn(Future.successful(token))
    when(authorization.authorize(None)).thenReturn(Future.failed(new IllegalArgumentException("Missing token")))
    when(permissions.all(CampusId("ald"))).thenReturn(Future.successful(Some((person, admin))))
    when(service.create(main, preview, Set(moduleId), title, person)).thenReturn(Future.successful(mrUrl))
    when(service.changes()).thenReturn(Future.successful((main, preview, Nil)))
    when(service.title()).thenReturn(Future.successful(title))
  }

  private def request(method: String = POST, path: String = url, json: JsValue = body) =
    route(app, FakeRequest(method, path).withHeaders(AUTHORIZATION -> "Bearer test").withJsonBody(json)).get

  protected override def afterEach(): Unit = {
    try verifyNoInteractions(http, mailer, gitCli, markdown)
    finally super.afterEach()
  }

  test("the Play application uses mocked external clients and a JDBC URL that PostgreSQL rejects") {
    assert(app.injector.instanceOf[SoftBigBangService] eq service)
    assert(app.injector.instanceOf[Authorization] eq authorization)
    assert(app.injector.instanceOf[PermissionRepository] eq permissions)
    assert(app.injector.instanceOf[WSClient] eq http)
    assert(app.injector.instanceOf[MailerClient] eq mailer)
    assert(app.injector.instanceOf[GitCLI] eq gitCli)
    assert(app.injector.instanceOf[MarkdownCLI] eq markdown)
    val configuration = app.configuration
    configuration.get[String]("slick.dbs.default.db.url") shouldBe "jdbc:disabled:tests"
    new org.postgresql.Driver().acceptsURL(configuration.get[String]("slick.dbs.default.db.url")) shouldBe false
    configuration.get[String]("slick.dbs.default.db.connectionPool") shouldBe "disabled"
    configuration.get[Boolean]("play.evolutions.db.default.enabled") shouldBe false
    configuration.get[Boolean]("play.evolutions.db.default.autoApply") shouldBe false
  }

  for (method, path) <- Seq((GET, url), (GET, s"$url/title"), (POST, url)) do {
    test(s"$method $path rejects unauthenticated requests before accessing permissions or publication") {
      val response = route(app, FakeRequest(method, path).withJsonBody(body)).get

      status(response) shouldBe UNAUTHORIZED
      (contentAsJson(response) \ "type").as[String] shouldBe "unauthorized"
      verifyNoInteractions(permissions, service)
    }

    test(s"$method $path requires database admin permissions even when the user token has an admin role") {
      when(permissions.all(CampusId("ald")))
        .thenReturn(Future.successful(Some((person, Permissions(Map(PermissionType.Module -> Set("inf1")))))))

      val response = request(method, path)

      status(response) shouldBe FORBIDDEN
      (contentAsJson(response) \ "type").as[String] shouldBe "forbidden"
      verifyNoInteractions(service)
    }
  }

  test("an inactive administrator cannot publish") {
    when(permissions.all(CampusId("ald"))).thenReturn(Future.successful(Some((person.copy(isActive = false), admin))))

    status(request()) shouldBe BAD_REQUEST

    verifyNoInteractions(service)
  }

  test("an unknown authenticated user cannot publish") {
    when(permissions.all(CampusId("ald"))).thenReturn(Future.successful(None))

    status(request()) shouldBe BAD_REQUEST

    verifyNoInteractions(service)
  }

  test("an administrator publishes the exact selected revisions with their resolved identity and a trimmed title") {
    val response = request(json = body ++ Json.obj("title" -> s"  $title  "))

    status(response) shouldBe CREATED
    contentAsJson(response) shouldBe Json.obj("mergeRequestUrl" -> mrUrl)
    verify(service).create(main, preview, Set(moduleId), title, person)
    verifyNoMoreInteractions(service)
  }

  test("the comparison response preserves revisions, change flags and nullable metadata for an unknown module") {
    val knownPath   = s"modules/$moduleId.md"
    val unknownPath = s"modules/$otherId.md"
    val knownDiff   = CompareDiff(knownPath, knownPath, false, false, false, "@@ change @@")
    val unknownDiff = CompareDiff(unknownPath, unknownPath, true, false, false, "")
    when(service.changes()).thenReturn(
      Future.successful(
        (
          main,
          preview,
          List(
            (moduleId, Some(ModuleCore(moduleId, "Known module", "KNOWN")), ModuleChange.Modified, knownDiff),
            (otherId, None, ModuleChange.Added, unknownDiff)
          )
        )
      )
    )

    val response = request(GET)

    status(response) shouldBe OK
    contentAsJson(response) shouldBe Json.obj(
      "mainSha"    -> main.value,
      "previewSha" -> preview.value,
      "modules"    -> Json.arr(
        Json.obj(
          "id"     -> moduleId.toString,
          "title"  -> "Known module",
          "abbrev" -> "KNOWN",
          "change" -> "modified",
          "file"   -> Json.obj(
            "oldPath"     -> knownPath,
            "newPath"     -> knownPath,
            "newFile"     -> false,
            "renamedFile" -> false,
            "deletedFile" -> false,
            "diff"        -> "@@ change @@"
          )
        ),
        Json.obj(
          "id"     -> otherId.toString,
          "title"  -> JsNull,
          "abbrev" -> JsNull,
          "change" -> "added",
          "file"   -> Json.obj(
            "oldPath"     -> unknownPath,
            "newPath"     -> unknownPath,
            "newFile"     -> true,
            "renamedFile" -> false,
            "deletedFile" -> false,
            "diff"        -> ""
          )
        )
      )
    )
    verify(service).changes()
    verifyNoMoreInteractions(service)
  }

  test("an administrator can load a title suggestion through its route") {
    val response = request(GET, s"$url/title")

    status(response) shouldBe OK
    contentAsJson(response) shouldBe Json.obj("title" -> title)
    verify(service).title()
    verifyNoMoreInteractions(service)
  }

  for (label, invalid) <- Seq(
      "empty selection"      -> (body ++ Json.obj("modules" -> Json.arr())),
      "invalid module ID"    -> (body ++ Json.obj("modules" -> Json.arr("not-a-uuid"))),
      "blank title"          -> (body ++ Json.obj("title" -> " \n\t ")),
      "short main SHA"       -> (body ++ Json.obj("mainSha" -> "1234")),
      "nonhex preview SHA"   -> (body ++ Json.obj("previewSha" -> ("z" * 40))),
      "missing main SHA"     -> (body - "mainSha"),
      "missing preview SHA"  -> (body - "previewSha"),
      "wrong selection type" -> (body ++ Json.obj("modules" -> moduleId.toString))
    )
  do
    test(s"invalid input returns 400 without invoking publication: $label") {
      status(request(json = invalid)) shouldBe BAD_REQUEST

      verifyNoInteractions(service)
    }

  test("malformed JSON returns 400 without invoking publication") {
    val response = route(
      app,
      FakeRequest(POST, url)
        .withHeaders(AUTHORIZATION -> "Bearer test", CONTENT_TYPE -> "application/json")
        .withBody("{broken")
    ).get

    status(response) shouldBe BAD_REQUEST
    verifyNoInteractions(service)
  }

  test("a pending publication returns 409 with the production client-error response") {
    when(service.create(main, preview, Set(moduleId), title, person)).thenReturn(Future.failed(new PendingException))

    val response = request()

    status(response) shouldBe CONFLICT
    (contentAsJson(response) \ "type").as[String] shouldBe "client_error"
    UUID.fromString((contentAsJson(response) \ "message").as[String])
    verify(service).create(main, preview, Set(moduleId), title, person)
  }

  test("a rejected selection returns 400 rather than a publication validation error") {
    when(service.create(main, preview, Set(moduleId), title, person))
      .thenReturn(Future.failed(new IllegalArgumentException("Selection contains modules without changes")))

    val response = request()

    status(response) shouldBe BAD_REQUEST
    (contentAsJson(response) \ "type").as[String] shouldBe "bad_request"
  }

  test("changed modules return 412 with exactly their IDs so the frontend can name them") {
    val error = new ModulesChangedException(Set(moduleId, otherId))
    when(service.create(main, preview, Set(moduleId, otherId), title, person)).thenReturn(Future.failed(error))

    val response = request(json = body ++ Json.obj("modules" -> Json.arr(moduleId.toString, otherId.toString)))
    val json     = contentAsJson(response)

    status(response) shouldBe PRECONDITION_FAILED
    (json \ "type").as[String] shouldBe "modules_changed"
    (json \ "message").as[String] shouldBe error.getMessage
    (json \ "modules").as[Seq[UUID]] should contain theSameElementsAs Seq(moduleId, otherId)
    verify(service).create(main, preview, Set(moduleId, otherId), title, person)
  }

  test("invalid modules return 422 with the complete validation diagnostic even in production") {
    val diagnostic = s"Module $moduleId: missing prerequisite\n\nModule $otherId: multiple parents"
    when(service.create(main, preview, Set(moduleId), title, person))
      .thenReturn(Future.failed(new InvalidModulesException(diagnostic)))

    val response = request()

    status(response) shouldBe UNPROCESSABLE_ENTITY
    contentAsJson(response) shouldBe Json.obj("type" -> "invalid_modules", "message" -> diagnostic)
  }

  for method <- Seq(GET, POST) do
    test(s"$method reports an incomplete comparison as 502 with a usable diagnostic") {
      val error = new IncompleteComparisonException
      when(service.changes()).thenReturn(Future.failed(error))
      when(service.create(main, preview, Set(moduleId), title, person)).thenReturn(Future.failed(error))

      val response = request(method)

      status(response) shouldBe BAD_GATEWAY
      contentAsJson(response) shouldBe Json.obj("type" -> "incomplete_comparison", "message" -> error.getMessage)
    }
}
