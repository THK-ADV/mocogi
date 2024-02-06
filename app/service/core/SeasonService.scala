package service.core

import database.repo.core.SeasonRepository
import models.core.Season
import parsing.core.{FileParser, SeasonFileParser}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class SeasonService @Inject() (
    val repo: SeasonRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[Season] {
  override def fileParser: FileParser[Season] = SeasonFileParser
}
