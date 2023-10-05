package providers

import play.api.Configuration

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.util.Try

@Singleton
final class ConfigReader @Inject() (config: Configuration) {

  def htmlCmd: String = string("pandoc.htmlCmd")

  def pdfCmd: String = string("pandoc.pdfCmd")

  def outputFolderPath: String = string("pandoc.outputFolderPath")

  def deOutputFolderPath: String = string("pandoc.deOutputFolderPath")

  def enOutputFolderPath: String = string("pandoc.enOutputFolderPath")

  def gitToken: Option[UUID] = config
    .getOptional[String]("git.token")
    .flatMap(s => Try(UUID.fromString(s)).toOption)

  def moduleModeToken: Option[UUID] = config
    .getOptional[String]("git.moduleModeToken")
    .flatMap(s => Try(UUID.fromString(s)).toOption)

  def accessToken: String = string("git.accessToken")

  def baseUrl: String = string("git.baseUrl")

  def mainBranch: String = string("git.mainBranch")

  def draftBranch: String = string("git.draftBranch")

  def modulesRootFolder: String = string("git.modulesRootFolder")

  def coreRootFolder: String = string("git.coreRootFolder")

  def projectId: Int = int("git.projectId")

  def kafkaServerUrl: String = string("kafka.serverUrl")

  def kafkaApplicationId: String = string("kafka.applicationId")

  def moduleKeysToReviewFromSgl: Seq[String] = list("moduleKeysToReview.sgl")

  def moduleKeysToReviewFromPav: Seq[String] = list("moduleKeysToReview.pav")

  def autoApprovedLabel: String = string("git.autoApprovedLabel")

  def reviewApprovedLabel: String = string("git.reviewApprovedLabel")

  private def list(key: String): Seq[String] =
    if (config.has(key)) config.get[Seq[String]](key)
    else throw new Throwable(s"key $key must be set in application.conf")

  private def string(key: String): String =
    config.getOptional[String](key) match {
      case Some(value) if value.nonEmpty => value
      case _ => throw new Throwable(s"$key must be set")
    }

  private def int(key: String): Int =
    config.getOptional[Int](key) match {
      case Some(value) => value
      case _           => throw new Throwable(s"$key must be set")
    }
}
