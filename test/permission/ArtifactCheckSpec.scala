package permission

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.Token
import auth.TokenRequest
import controllers.actions.UserRequest
import models.core.Identity
import models.EmploymentType.Unknown
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.ActionFilter
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.Environment
import security.ClientErrorResponse

final class ArtifactCheckSpec extends AnyWordSpec with Matchers with ScalaFutures {
  private given ExecutionContext = ExecutionContext.global

  private val artifactCheck = new ArtifactCheck {
    protected override val clientErrors: ClientErrorResponse = ClientErrorResponse(Environment.simple())
    protected implicit override val ctx: ExecutionContext    = ExecutionContext.global
  }

  private val person = Identity.Person(
    id = "person",
    lastname = "Person",
    firstname = "Test",
    title = "",
    faculties = Nil,
    abbreviation = "",
    campusId = Some("test-person"),
    isActive = true,
    employmentType = Unknown,
    websiteUrl = None
  )

  private def request(permissions: Permissions): UserRequest[AnyContentAsEmpty.type] = {
    val token = Token.UserToken("Test", "Person", "test-person", "test@example.com", Set.empty)
    UserRequest(person, permissions, TokenRequest(FakeRequest(), token))
  }

  private def result(filter: ActionFilter[UserRequest], permissions: Permissions) =
    filter.invokeBlock(request(permissions), _ => Future.successful(Ok)).futureValue

  "canPreviewArtifact" should {
    "allow preview and create permissions for the requested PO" in {
      val preview = Permissions(Map(PermissionType.ArtifactsPreview -> Set("inf_inf2")))
      val create  = Permissions(Map(PermissionType.ArtifactsCreate -> Set("inf_inf2")))

      result(artifactCheck.canPreviewArtifact("inf_inf2"), preview).header.status shouldBe 200
      result(artifactCheck.canPreviewArtifact("inf_inf2"), create).header.status shouldBe 200
    }

    "deny a different PO in the same study program" in {
      val permissions = Permissions(Map(PermissionType.ArtifactsPreview -> Set("inf_inf2")))

      result(artifactCheck.canPreviewArtifact("inf_inf3"), permissions).header.status shouldBe 403
    }
  }

  "canCreateArtifact" should {
    "allow only create permission for the requested PO" in {
      val preview = Permissions(Map(PermissionType.ArtifactsPreview -> Set("inf_inf2")))
      val create  = Permissions(Map(PermissionType.ArtifactsCreate -> Set("inf_inf2")))

      result(artifactCheck.canCreateArtifact("inf_inf2"), preview).header.status shouldBe 403
      result(artifactCheck.canCreateArtifact("inf_inf2"), create).header.status shouldBe 200
      result(artifactCheck.canCreateArtifact("inf_inf3"), create).header.status shouldBe 403
    }

    "allow administrators" in {
      val admin = Permissions(Map(PermissionType.Admin -> Set.empty))

      result(artifactCheck.canCreateArtifact("any-po"), admin).header.status shouldBe 200
    }
  }
}
