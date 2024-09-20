package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.core.SeasonRepository
import models.core.Season
import parser.Parser
import parsing.core.SeasonFileParser

@Singleton
final class SeasonService @Inject() (
    val repo: SeasonRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[Season] {
  override def fileParser: Parser[List[Season]] = SeasonFileParser.parser()
}
