package providers

import ops.ConfigurationOps.Ops
import play.api.Configuration

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.util.Try

@Singleton
final class ConfigReader @Inject() (config: Configuration) {

  def htmlCmd: String = config.nonEmptyString("pandoc.htmlCmd")

  def pdfCmd: String = config.nonEmptyString("pandoc.pdfCmd")

  def texCmd: String = config.nonEmptyString("pandoc.texCmd")

  def tmpFolderPath: String = config.nonEmptyString("play.temporaryFile.dir")

  def outputFolderPath: String =
    config.nonEmptyString("pandoc.outputFolderPath")

  def deOutputFolderPath: String =
    config.nonEmptyString("pandoc.deOutputFolderPath")

  def enOutputFolderPath: String =
    config.nonEmptyString("pandoc.enOutputFolderPath")

  def moduleCatalogFolderPath: String = config.nonEmptyString(
    "pandoc.moduleCatalogFolderPath"
  )

  def wpfCatalogueFolderPath: String = config.nonEmptyString(
    "pandoc.wpfCatalogueFolderPath"
  )

  def gitToken: Option[UUID] = config
    .getOptional[String]("git.token")
    .flatMap(s => Try(UUID.fromString(s)).toOption)

  def moduleModeToken: Option[UUID] = config
    .getOptional[String]("git.moduleModeToken")
    .flatMap(s => Try(UUID.fromString(s)).toOption)

  def accessToken: String = config.nonEmptyString("git.accessToken")

  def baseUrl: String = config.nonEmptyString("git.baseUrl")

  def mainBranch: String = config.nonEmptyString("git.mainBranch")

  def draftBranch: String = config.nonEmptyString("git.draftBranch")

  def modulesRootFolder: String = config.nonEmptyString("git.modulesRootFolder")

  def coreRootFolder: String = config.nonEmptyString("git.coreRootFolder")

  def moduleCatalogRootFolder: String =
    config.nonEmptyString("git.moduleCatalogRootFolder")

  def projectId: Int = config.int("git.projectId")

  def kafkaServerUrl: String = config.nonEmptyString("kafka.serverUrl")

  def kafkaApplicationId: String = config.nonEmptyString("kafka.applicationId")

  def moduleKeysToReviewFromSgl: Seq[String] =
    config.list("moduleKeysToReview.sgl")

  def moduleKeysToReviewFromPav: Seq[String] =
    config.list("moduleKeysToReview.pav")

  def autoApprovedLabel: String = config.nonEmptyString("git.autoApprovedLabel")

  def reviewApprovedLabel: String =
    config.nonEmptyString("git.reviewApprovedLabel")

  def repoPath: String = config.nonEmptyString("glab.repoPath")

  def mcPath: String = config.nonEmptyString("glab.mcPath")

  def pushScriptPath: String = config.nonEmptyString("glab.pushScriptPath")

  def switchBranchScriptPath: String = config.nonEmptyString(
    "glab.switchBranchScriptPath"
  )

  def diffPreviewScriptPath: String = config.nonEmptyString(
    "glab.diffPreviewScriptPath"
  )
}
