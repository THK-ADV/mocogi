package service.core

import database.repo.core.StatusRepository
import models.core.ModuleStatus
import parser.Parser
import parsing.core.StatusFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class StatusService @Inject() (
    val repo: StatusRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleStatus] {
  override def fileParser: Parser[List[ModuleStatus]] =
    StatusFileParser.parser()
}
