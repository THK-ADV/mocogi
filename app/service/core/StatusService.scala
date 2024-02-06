package service.core

import database.repo.core.StatusRepository
import models.core.Status
import parsing.core.{FileParser, StatusFileParser}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait StatusService extends SimpleYamlService[Status]

@Singleton
final class StatusServiceImpl @Inject() (
    val repo: StatusRepository,
    val ctx: ExecutionContext
) extends StatusService {
  override def fileParser: FileParser[Status] = StatusFileParser
}
