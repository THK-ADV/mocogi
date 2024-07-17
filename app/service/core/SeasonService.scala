package service.core

import database.repo.core.SeasonRepository
import models.core.Season
import parser.Parser
import parsing.core.SeasonFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class SeasonService @Inject() (
    val repo: SeasonRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[Season] {
  override def fileParser: Parser[List[Season]] = SeasonFileParser.parser()
}
