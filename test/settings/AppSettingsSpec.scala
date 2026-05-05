package settings

import java.time.LocalDate
import java.util.UUID

import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.Configuration

final class AppSettingsSpec extends AnyFlatSpec with Matchers:

  private def conf(extra: String = ""): Configuration =
    val base =
      """
        |play.temporaryFile.dir = "tmp"
        |pandoc.wordCmd = "word"
        |pandoc.texCmd = "tex"
        |pandoc.mcIntroPath = "intro"
        |pandoc.mcAssetsPath = "assets"
        |pandoc.examListOutputFolderPath = "exam"
        |pandoc.moduleCatalogOutputFolderPath = "cat"
        |mail.sender = "a@b.c"
        |mail.reviewUrl = "https://example.com/review"
        |mail.editUrl = "https://example.com/edit"
        |keycloak.jwksUrl = "https://example.com/jwks"
        |keycloak.issuer = "https://example.com/realm"
        |git.repoUrl = "https://git.example/repo"
        |git.token = "a0eea988-04f6-4b51-80a0-345f520718a7"
        |git.localGitFolderPath = "/tmp/git"
        |git.accessToken = "secret-token"
        |git.baseUrl = "https://api.example"
        |git.projectId = 2124
        |git.mainBranch = "main"
        |git.draftBranch = "draft"
        |git.modulesFolder = "modules"
        |git.coreFolder = "core"
        |git.moduleCatalogsFolder = "catalogs"
        |git.moduleCompanionFolder = "companions"
        |git.autoApprovedLabel = "auto"
        |git.reviewRequiredLabel = "review"
        |git.fastForwardLabel = "ff"
        |git.bigBangLabel = "bb"
        |git.moduleCatalogLabel = "mc"
        |git.defaultEmail = "bot@example.com"
        |git.defaultUser = "bot"
        |git.historySince = "1990-01-01"
        |moduleKeysToReview.pav = [ "a", "b" ]
        |""".stripMargin
    Configuration(ConfigFactory.parseString(base + extra))

  "AppSettings.load" should "parse a valid configuration" in {
    val s = AppSettings.load(conf())
    s.git.repoUrl shouldBe "https://git.example/repo"
    s.git.webhookToken shouldBe UUID.fromString("a0eea988-04f6-4b51-80a0-345f520718a7")
    SecretString.unwrap(s.git.accessToken) shouldBe "secret-token"
    s.git.projectId shouldBe 2124
    s.moduleKeysToReview.pavModuleKeys shouldBe Seq("a", "b")
    s.play.tmpDir shouldBe "tmp"
    s.git.historySince shouldBe LocalDate.of(1990, 1, 1)
  }

  it should "accept git.projectId as a numeric string" in {
    val s = AppSettings.load(conf("git.projectId = \"99\"\n"))
    s.git.projectId shouldBe 99
  }

  it should "reject an invalid git.token UUID" in {
    assertThrows[IllegalArgumentException] {
      AppSettings.load(conf("git.token = \"not-a-uuid\"\n"))
    }
  }

  it should "fail when a required key is missing" in {
    assertThrows[Exception] {
      AppSettings.load(Configuration(ConfigFactory.parseString("play.temporaryFile.dir = x")))
    }
  }
