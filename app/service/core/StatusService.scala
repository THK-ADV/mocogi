package service.core

import database.repo.StatusRepository
import models.core.Status
import parsing.core.StatusFileParser

import javax.inject.{Inject, Singleton}

trait StatusService extends SimpleYamlService[Status]

@Singleton
final class StatusServiceImpl @Inject() (
    val repo: StatusRepository,
    val parser: StatusFileParser
) extends StatusService
