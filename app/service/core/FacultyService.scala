package service.core

import database.repo.FacultyRepository
import models.core.Faculty
import parsing.core.FacultyFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait FacultyService extends SimpleYamlService[Faculty]

@Singleton
final class FacultyServiceImpl @Inject() (
    val repo: FacultyRepository,
    val ctx: ExecutionContext
) extends FacultyService {
  override def fileParser = FacultyFileParser
}
