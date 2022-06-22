package providers

import play.api.Configuration

import javax.inject.{Inject, Singleton}

@Singleton
final class ConfigReader @Inject() (config: Configuration) {

  def seasons: String = string("filePaths.seasons")

  def persons: String = string("filePaths.persons")

  def status: String = string("filePaths.status")

  def moduleTypes: String = string("filePaths.moduleTypes")

  def locations: String = string("filePaths.locations")

  def languages: String = string("filePaths.languages")

  def assessmentMethods: String = string("filePaths.assessmentMethods")

  private def string(key: String): String =
    config.getOptional[String](key) match {
      case Some(value) if value.nonEmpty => value
      case _ => throw new Throwable(s"$key must be set")
    }
}
