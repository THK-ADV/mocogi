package service

import basedata.Season
import database.repo.SeasonRepository
import parsing.base.SeasonFileParser

import javax.inject.{Inject, Singleton}

trait SeasonService extends SimpleYamlService[Season]

@Singleton
final class SeasonServiceImpl @Inject() (
    val repo: SeasonRepository,
    val parser: SeasonFileParser
) extends SeasonService
