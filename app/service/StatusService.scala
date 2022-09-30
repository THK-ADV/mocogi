package service

import basedata.Status
import database.repo.StatusRepository
import parsing.metadata.file.StatusFileParser

import javax.inject.{Inject, Singleton}

trait StatusService extends YamlService[Status]

@Singleton
final class StatusServiceImpl @Inject() (
    val repo: StatusRepository,
    val parser: StatusFileParser
) extends StatusService
