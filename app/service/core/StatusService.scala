package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.core.StatusRepository
import models.core.ModuleStatus
import parser.Parser
import parsing.core.StatusFileParser

@Singleton
final class StatusService @Inject() (
    val repo: StatusRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleStatus] {
  override def fileParser: Parser[List[ModuleStatus]] =
    StatusFileParser.parser()
}
