package service.core

import database.repo.core.LanguageRepository
import models.core.ModuleLanguage
import parser.Parser
import parsing.core.LanguageFileParser

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class LanguageService @Inject() (
    val repo: LanguageRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleLanguage] {
  override def fileParser: Parser[List[ModuleLanguage]] =
    LanguageFileParser.parser()
}
