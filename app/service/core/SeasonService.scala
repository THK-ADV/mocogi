package service.core

import database.repo.SeasonRepository
import models.core.Season
import parsing.core.SeasonFileParser

import javax.inject.{Inject, Singleton}

trait SeasonService extends SimpleYamlService[Season]

@Singleton
final class SeasonServiceImpl @Inject() (
    val repo: SeasonRepository,
    val parser: SeasonFileParser
) extends SeasonService
