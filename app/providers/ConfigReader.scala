package providers

import play.api.Configuration

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.util.Try

@Singleton
final class ConfigReader @Inject() (config: Configuration) {

  def htmlCmd: String = nonEmptyString("pandoc.htmlCmd")

  def pdfCmd: String = nonEmptyString("pandoc.pdfCmd")

  def texCmd: String = nonEmptyString("pandoc.texCmd")

  def tmpFolderPath: String = nonEmptyString("play.temporaryFile.dir")

  def outputFolderPath: String = nonEmptyString("pandoc.outputFolderPath")

  def deOutputFolderPath: String = nonEmptyString("pandoc.deOutputFolderPath")

  def enOutputFolderPath: String = nonEmptyString("pandoc.enOutputFolderPath")

  def moduleCompendiumFolderPath: String = nonEmptyString(
    "pandoc.moduleCompendiumFolderPath"
  )

  def wpfCatalogueFolderPath: String = nonEmptyString(
    "pandoc.wpfCatalogueFolderPath"
  )

  def gitToken: Option[UUID] = config
    .getOptional[String]("git.token")
    .flatMap(s => Try(UUID.fromString(s)).toOption)

  def moduleModeToken: Option[UUID] = config
    .getOptional[String]("git.moduleModeToken")
    .flatMap(s => Try(UUID.fromString(s)).toOption)

  def accessToken: String = nonEmptyString("git.accessToken")

  def baseUrl: String = nonEmptyString("git.baseUrl")

  def mainBranch: String = nonEmptyString("git.mainBranch")

  def draftBranch: String = nonEmptyString("git.draftBranch")

  def modulesRootFolder: String = nonEmptyString("git.modulesRootFolder")

  def coreRootFolder: String = nonEmptyString("git.coreRootFolder")

  def projectId: Int = int("git.projectId")

  def kafkaServerUrl: String = nonEmptyString("kafka.serverUrl")

  def kafkaApplicationId: String = nonEmptyString("kafka.applicationId")

  def moduleKeysToReviewFromSgl: Seq[String] = list("moduleKeysToReview.sgl")

  def moduleKeysToReviewFromPav: Seq[String] = list("moduleKeysToReview.pav")

  def autoApprovedLabel: String = nonEmptyString("git.autoApprovedLabel")

  def reviewApprovedLabel: String = nonEmptyString("git.reviewApprovedLabel")

  def repoPath: String = nonEmptyString("glab.repoPath")

  def mcPath: String = nonEmptyString("glab.mcPath")

  def pushScriptPath: Option[String] = emptyString("glab.pushScriptPath")

  def switchBranchScriptPath: String = nonEmptyString(
    "glab.switchBranchScriptPath"
  )

  def diffPreviewScriptPath: String = nonEmptyString(
    "glab.diffPreviewScriptPath"
  )

  private def list(key: String): Seq[String] =
    if (config.has(key)) config.get[Seq[String]](key)
    else throw new Throwable(s"key $key must be set in application.conf")

  private def nonEmptyString(key: String): String =
    config.getOptional[String](key) match {
      case Some(value) if value.nonEmpty => value
      case _ => throw new Throwable(s"$key must be set")
    }

  private def emptyString(key: String): Option[String] =
    config.getOptional[String](key) match {
      case Some(value) if value.nonEmpty => Some(value)
      case _                             => None
    }

  private def int(key: String): Int =
    config.getOptional[Int](key) match {
      case Some(value) => value
      case _           => throw new Throwable(s"$key must be set")
    }
}
