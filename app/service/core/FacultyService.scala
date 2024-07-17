package service.core

import database.repo.core.FacultyRepository
import models.core.Faculty
import parser.Parser
import parsing.core.FacultyFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class FacultyService @Inject() (
    val repo: FacultyRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[Faculty] {
  override def fileParser: Parser[List[Faculty]] = FacultyFileParser.parser()
}
