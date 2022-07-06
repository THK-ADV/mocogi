package service

import database.repo.StatusRepository
import parsing.metadata.file.StatusFileParser
import parsing.types.Status

import javax.inject.{Inject, Singleton}

trait StatusService extends YamlService[Status]

@Singleton
final class StatusServiceImpl @Inject() (
    val repo: StatusRepository,
    val parser: StatusFileParser
) extends StatusService
