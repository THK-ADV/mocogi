package service.core

import database.repo.LocationRepository
import models.core.Location
import parsing.core.{FileParser, LocationFileParser}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait LocationService extends SimpleYamlService[Location]

@Singleton
final class LocationServiceImpl @Inject() (
    val repo: LocationRepository,
    val ctx: ExecutionContext
) extends LocationService {

  override def fileParser: FileParser[Location] = LocationFileParser
}
