package service.core

import database.repo.core.LocationRepository
import models.core.ModuleLocation
import parsing.core.{FileParser, LocationFileParser}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait LocationService extends SimpleYamlService[ModuleLocation]

@Singleton
final class LocationServiceImpl @Inject() (
    val repo: LocationRepository,
    val ctx: ExecutionContext
) extends LocationService {

  override def fileParser: FileParser[ModuleLocation] = LocationFileParser
}
