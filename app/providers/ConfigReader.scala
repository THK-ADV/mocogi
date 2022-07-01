package providers

import play.api.Configuration

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.util.Try

@Singleton
final class ConfigReader @Inject() (config: Configuration) {

  def seasons: String = string("filePaths.seasons")

  def persons: String = string("filePaths.persons")

  def status: String = string("filePaths.status")

  def moduleTypes: String = string("filePaths.moduleTypes")

  def locations: String = string("filePaths.locations")

  def languages: String = string("filePaths.languages")

  def assessmentMethods: String = string("filePaths.assessmentMethods")

  def htmlCmd: String = string("pandoc.htmlCmd")

  def pdfCmd: String = string("pandoc.pdfCmd")

  def gitToken: Option[UUID] = config
    .getOptional[String]("git.token")
    .flatMap(s => Try(UUID.fromString(s)).toOption)

  def accessToken: String = string("git.accessToken")

  def baseUrl: String = string("git.baseUrl")

  private def string(key: String): String =
    config.getOptional[String](key) match {
      case Some(value) if value.nonEmpty => value
      case _ => throw new Throwable(s"$key must be set")
    }
}
