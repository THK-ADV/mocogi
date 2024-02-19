package service.core

import database.repo.core.LocationRepository
import models.core.ModuleLocation
import parsing.core.{FileParser, LocationFileParser}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class LocationService @Inject() (
    val repo: LocationRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleLocation] {
  override def fileParser: FileParser[ModuleLocation] = LocationFileParser
}
