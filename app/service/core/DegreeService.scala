package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.core.DegreeRepository
import models.core.Degree
import parsing.core.DegreeFileParser

@Singleton
final class DegreeService @Inject() (
    val repo: DegreeRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[Degree] {
  override def fileParser = DegreeFileParser.parser()
}
