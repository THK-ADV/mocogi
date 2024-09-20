package service.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext

import database.repo.core.LanguageRepository
import models.core.ModuleLanguage
import parser.Parser
import parsing.core.LanguageFileParser

@Singleton
final class LanguageService @Inject() (
    val repo: LanguageRepository,
    val ctx: ExecutionContext
) extends SimpleYamlService[ModuleLanguage] {
  override def fileParser: Parser[List[ModuleLanguage]] =
    LanguageFileParser.parser()
}
