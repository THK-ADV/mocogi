package service

import database.repo.LocationRepository
import parsing.metadata.file.LocationFileParser
import parsing.types.Location

import javax.inject.{Inject, Singleton}

trait LocationService extends YamlService[Location]

@Singleton
final class LocationServiceImpl @Inject() (
    val repo: LocationRepository,
    val parser: LocationFileParser
) extends LocationService
