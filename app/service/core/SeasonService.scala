package service.core

import database.repo.SeasonRepository
import models.core.Season
import parsing.core.{FileParser, SeasonFileParser}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait SeasonService extends SimpleYamlService[Season]

@Singleton
final class SeasonServiceImpl @Inject() (
    val repo: SeasonRepository,
    val ctx: ExecutionContext
) extends SeasonService {
  override def fileParser: FileParser[Season] = SeasonFileParser
}
