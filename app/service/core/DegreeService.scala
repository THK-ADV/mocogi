package service.core

import database.repo.DegreeRepository
import models.core.Degree
import parsing.core.DegreeFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait DegreeService extends SimpleYamlService[Degree]

@Singleton
final class DegreeServiceImpl @Inject() (
    val repo: DegreeRepository,
    val ctx: ExecutionContext
) extends DegreeService {
  override def fileParser = DegreeFileParser
}
