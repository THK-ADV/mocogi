package providers

import ops.ConfigurationOps.Ops
import play.api.Configuration

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.Try

@Singleton
final class ConfigReader @Inject() (config: Configuration) {

  val res = for {
    trigger <- config.underlying
      .getConfigList("bigbang.trigger")
      .iterator()
      .asScala
    date = trigger.getString("date")
    semester = trigger.getString("semester")
  } yield (date, semester)

  res.foreach(println)

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

  def moduleCatalogOutputFolderPath: String = config.nonEmptyString(
    "pandoc.moduleCatalogOutputFolderPath"
  )

  def electivesCatalogOutputFolderPath: String = config.nonEmptyString(
    "pandoc.electivesCatalogOutputFolderPath"
  )

  def gitToken: Option[UUID] = config
    .getOptional[String]("git.token")
    .flatMap(s => Try(UUID.fromString(s)).toOption)

  def accessToken: String = config.nonEmptyString("git.accessToken")

  def baseUrl: String = config.nonEmptyString("git.baseUrl")

  def mainBranch: String = config.nonEmptyString("git.mainBranch")

  def draftBranch: String = config.nonEmptyString("git.draftBranch")

  def gitModulesFolder: String = config.nonEmptyString("git.modulesFolder")

  def gitCoreFolder: String = config.nonEmptyString("git.coreFolder")

  def gitModuleCatalogsFolder: String =
    config.nonEmptyString("git.moduleCatalogsFolder")

  def projectId: Int = config.int("git.projectId")

  def moduleKeysToReviewFromSgl: Seq[String] =
    config.list("moduleKeysToReview.sgl")

  def moduleKeysToReviewFromPav: Seq[String] =
    config.list("moduleKeysToReview.pav")

  def autoApprovedLabel: String = config.nonEmptyString("git.autoApprovedLabel")

  def reviewRequiredLabel: String =
    config.nonEmptyString("git.reviewRequiredLabel")

  def bigBangLabel = config.nonEmptyString("git.bigBangLabel")

  def moduleCatalogLabel = config.nonEmptyString("git.moduleCatalogLabel")

  def defaultEmail = config.nonEmptyString("git.defaultEmail")

  def defaultUser = config.nonEmptyString("git.defaultUser")
}
