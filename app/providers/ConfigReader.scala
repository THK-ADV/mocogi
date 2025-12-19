package providers

import javax.inject.Inject
import javax.inject.Singleton

import ops.int
import ops.list
import ops.nonEmptyString
import play.api.Configuration

@Singleton
final class ConfigReader @Inject() (val config: Configuration) {

  def tmpFolderPath: String =
    config.nonEmptyString("play.temporaryFile.dir")

  def moduleCatalogOutputFolderPath: String =
    config.nonEmptyString("pandoc.moduleCatalogOutputFolderPath")

  def accessToken: String =
    config.nonEmptyString("git.accessToken")

  def baseUrl: String =
    config.nonEmptyString("git.baseUrl")

  def mainBranch: String =
    config.nonEmptyString("git.mainBranch")

  def draftBranch: String =
    config.nonEmptyString("git.draftBranch")

  def gitModulesFolder: String =
    config.nonEmptyString("git.modulesFolder")

  def gitCoreFolder: String =
    config.nonEmptyString("git.coreFolder")

  def gitModuleCatalogsFolder: String =
    config.nonEmptyString("git.moduleCatalogsFolder")

  def gitModuleCompanionFolder: String =
    config.nonEmptyString("git.moduleCompanionFolder")

  def projectId: Int = config.int("git.projectId")

  def moduleKeysToReviewFromPav: Seq[String] =
    config.list("moduleKeysToReview.pav")

  def autoApprovedLabel: String =
    config.nonEmptyString("git.autoApprovedLabel")

  def reviewRequiredLabel: String =
    config.nonEmptyString("git.reviewRequiredLabel")

  def fastForwardLabel: String =
    config.nonEmptyString("git.fastForwardLabel")

  def bigBangLabel =
    config.nonEmptyString("git.bigBangLabel")

  def moduleCatalogLabel =
    config.nonEmptyString("git.moduleCatalogLabel")

  def defaultEmail =
    config.nonEmptyString("git.defaultEmail")

  def defaultUser =
    config.nonEmptyString("git.defaultUser")
}
