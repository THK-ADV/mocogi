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

  def gitToken: Option[UUID] = config
    .getOptional[String]("git.token")
    .flatMap(s => Try(UUID.fromString(s)).toOption)

  def accessToken: String = string("git.accessToken")

  def baseUrl: String = string("git.baseUrl")

  def mainBranch: String = string("git.mainBranch")

  def modulesRootFolder: String = string("git.modulesRootFolder")

  def projectId: Int = int("git.projectId")

  def kafkaServerUrl: String = string("kafka.serverUrl")

  def kafkaApplicationId: String = string("kafka.applicationId")

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
