package service.core

import database.repo.LocationRepository
import models.core.Location
import parsing.core.LocationFileParser

import javax.inject.{Inject, Singleton}

trait LocationService extends SimpleYamlService[Location]

@Singleton
final class LocationServiceImpl @Inject() (
    val repo: LocationRepository,
    val parser: LocationFileParser
) extends LocationService
