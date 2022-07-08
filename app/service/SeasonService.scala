package service

import database.repo.SeasonRepository
import parsing.metadata.file.SeasonFileParser
import parsing.types.Season

import javax.inject.{Inject, Singleton}

trait SeasonService extends YamlService[Season]

@Singleton
final class SeasonServiceImpl @Inject() (
    val repo: SeasonRepository,
    val parser: SeasonFileParser
) extends SeasonService
