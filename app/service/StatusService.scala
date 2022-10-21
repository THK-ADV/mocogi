package service

import basedata.Status
import database.repo.StatusRepository
import parsing.base.StatusFileParser

import javax.inject.{Inject, Singleton}

trait StatusService extends YamlService[Status, Status]

@Singleton
final class StatusServiceImpl @Inject() (
    val repo: StatusRepository,
    val parser: StatusFileParser
) extends StatusService {
  override def toInput(output: Status) = output
}
