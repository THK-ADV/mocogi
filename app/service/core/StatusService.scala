package service.core

import database.repo.core.StatusRepository
import models.core.ModuleStatus
import parsing.core.{FileParser, StatusFileParser}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class StatusService @Inject() (
    val repo: StatusRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleStatus] {
  override def fileParser: FileParser[ModuleStatus] = StatusFileParser
}
