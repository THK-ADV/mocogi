package service.core

import database.repo.core.DegreeRepository
import models.core.Degree
import parsing.core.DegreeFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class DegreeService @Inject() (
    val repo: DegreeRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[Degree] {
  override def fileParser = DegreeFileParser
}
