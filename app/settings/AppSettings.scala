package settings

import java.util.UUID

import play.api.Configuration

final case class GitRepoSettings(
    repoUrl: String,
    webhookToken: UUID,
    localGitFolderPath: String,
    accessToken: SecretString,
    baseUrl: String,
    projectId: Int,
    mainBranch: String,
    draftBranch: String,
    modulesFolder: String,
    coreFolder: String,
    moduleCatalogsFolder: String,
    moduleCompanionFolder: String,
    autoApprovedLabel: String,
    reviewRequiredLabel: String,
    fastForwardLabel: String,
    bigBangLabel: String,
    moduleCatalogLabel: String,
    defaultEmail: String,
    defaultUser: String
)

final case class PandocSettings(
    wordCmd: String,
    texCmd: String,
    mcIntroPath: String,
    mcAssetsPath: String,
    examListOutputFolderPath: String,
    moduleCatalogOutputFolderPath: String
)

final case class MailSettings(sender: String, reviewUrl: String, editUrl: String)

final case class KeycloakSettings(jwksUrl: String, issuer: String)

final case class PlayPathsSettings(tmpDir: String)

final case class ModuleKeysToReviewSettings(pavModuleKeys: Seq[String])

/**
 * Single validated view of application.conf (fail-fast at load).
 *
 * Note: features currently read from this general settings aggregate. Once a feature
 * is stable, prefer introducing a derived, feature-specific settings representation
 * containing only the required properties for that feature.
 */
final case class AppSettings(
    play: PlayPathsSettings,
    pandoc: PandocSettings,
    mail: MailSettings,
    keycloak: KeycloakSettings,
    git: GitRepoSettings,
    moduleKeysToReview: ModuleKeysToReviewSettings
)

object AppSettings {

  def load(configuration: Configuration): AppSettings = {
    val gitProjectId = gitProjectIdInt(configuration)

    AppSettings(
      play = PlayPathsSettings(tmpDir = nonEmptyString(configuration, "play.temporaryFile.dir")),
      pandoc = PandocSettings(
        wordCmd = nonEmptyString(configuration, "pandoc.wordCmd"),
        texCmd = nonEmptyString(configuration, "pandoc.texCmd"),
        mcIntroPath = nonEmptyString(configuration, "pandoc.mcIntroPath"),
        mcAssetsPath = nonEmptyString(configuration, "pandoc.mcAssetsPath"),
        examListOutputFolderPath = nonEmptyString(configuration, "pandoc.examListOutputFolderPath"),
        moduleCatalogOutputFolderPath = nonEmptyString(configuration, "pandoc.moduleCatalogOutputFolderPath")
      ),
      mail = MailSettings(
        sender = nonEmptyString(configuration, "mail.sender"),
        reviewUrl = nonEmptyString(configuration, "mail.reviewUrl"),
        editUrl = nonEmptyString(configuration, "mail.editUrl")
      ),
      keycloak = KeycloakSettings(
        jwksUrl = nonEmptyString(configuration, "keycloak.jwksUrl"),
        issuer = nonEmptyString(configuration, "keycloak.issuer")
      ),
      git = GitRepoSettings(
        repoUrl = nonEmptyString(configuration, "git.repoUrl"),
        webhookToken = parseUuid(configuration, "git.token"),
        localGitFolderPath = nonEmptyString(configuration, "git.localGitFolderPath"),
        accessToken = SecretString.unsafe(nonEmptyString(configuration, "git.accessToken")),
        baseUrl = nonEmptyString(configuration, "git.baseUrl"),
        projectId = gitProjectId,
        mainBranch = nonEmptyString(configuration, "git.mainBranch"),
        draftBranch = nonEmptyString(configuration, "git.draftBranch"),
        modulesFolder = nonEmptyString(configuration, "git.modulesFolder"),
        coreFolder = nonEmptyString(configuration, "git.coreFolder"),
        moduleCatalogsFolder = nonEmptyString(configuration, "git.moduleCatalogsFolder"),
        moduleCompanionFolder = nonEmptyString(configuration, "git.moduleCompanionFolder"),
        autoApprovedLabel = nonEmptyString(configuration, "git.autoApprovedLabel"),
        reviewRequiredLabel = nonEmptyString(configuration, "git.reviewRequiredLabel"),
        fastForwardLabel = nonEmptyString(configuration, "git.fastForwardLabel"),
        bigBangLabel = nonEmptyString(configuration, "git.bigBangLabel"),
        moduleCatalogLabel = nonEmptyString(configuration, "git.moduleCatalogLabel"),
        defaultEmail = nonEmptyString(configuration, "git.defaultEmail"),
        defaultUser = nonEmptyString(configuration, "git.defaultUser")
      ),
      moduleKeysToReview = ModuleKeysToReviewSettings(pavModuleKeys = list(configuration, "moduleKeysToReview.pav"))
    )
  }

  private def list(c: Configuration, key: String): Seq[String] =
    if c.has(key) then c.get[Seq[String]](key)
    else throw Exception(s"key $key must be set in application.conf")

  private def nonEmptyString(c: Configuration, key: String): String =
    c.getOptional[String](key) match {
      case Some(value) if value.nonEmpty => value
      case other                         => throw Exception(s"expected a non empty string for key $key, but found $other")
    }

  private def parseUuid(c: Configuration, key: String): UUID = {
    val raw = nonEmptyString(c, key)
    try UUID.fromString(raw)
    catch {
      case _: IllegalArgumentException =>
        throw IllegalArgumentException(s"key $key must be a valid UUID, got: $raw")
    }
  }

  private def parseProjectIdFromString(raw: String): Int =
    scala.util.Try(raw.toInt).getOrElse(throw IllegalArgumentException(s"git.projectId must be an integer, got: $raw"))

  /** HOCON may store project id as an int or as a numeric string. */
  private def gitProjectIdInt(c: Configuration): Int =
    c.getOptional[Int]("git.projectId") match {
      case Some(v) => v
      case None    =>
        c.getOptional[String]("git.projectId") match {
          case Some(s) => parseProjectIdFromString(s)
          case None    => throw Exception("git.projectId must be set")
        }
    }
}
