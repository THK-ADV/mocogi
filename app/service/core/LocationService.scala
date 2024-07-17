package service.core

import database.repo.core.LocationRepository
import models.core.ModuleLocation
import parser.Parser
import parsing.core.LocationFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class LocationService @Inject() (
    val repo: LocationRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleLocation] {
  override def fileParser: Parser[List[ModuleLocation]] =
    LocationFileParser.parser()
}
