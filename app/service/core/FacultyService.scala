package service.core

import database.repo.core.FacultyRepository
import models.core.Faculty
import parsing.core.FacultyFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class FacultyService @Inject() (
    val repo: FacultyRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[Faculty] {
  override def fileParser = FacultyFileParser
}
