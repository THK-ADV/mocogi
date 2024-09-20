package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.core.LocationRepository
import models.core.ModuleLocation
import parser.Parser
import parsing.core.LocationFileParser

@Singleton
final class LocationService @Inject() (
    val repo: LocationRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleLocation] {
  override def fileParser: Parser[List[ModuleLocation]] =
    LocationFileParser.parser()
}
