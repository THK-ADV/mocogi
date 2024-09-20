package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.core.FacultyRepository
import models.core.Faculty
import parser.Parser
import parsing.core.FacultyFileParser

@Singleton
final class FacultyService @Inject() (
    val repo: FacultyRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[Faculty] {
  override def fileParser: Parser[List[Faculty]] = FacultyFileParser.parser()

  def allIds() =
    repo.allIds()
}
