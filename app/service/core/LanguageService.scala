package service.core

import database.repo.core.LanguageRepository
import models.core.ModuleLanguage
import parsing.core.{FileParser, LanguageFileParser}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait LanguageService extends SimpleYamlService[ModuleLanguage]

@Singleton
final class LanguageServiceImpl @Inject() (
    val repo: LanguageRepository,
    val ctx: ExecutionContext
) extends LanguageService {
  override def fileParser: FileParser[ModuleLanguage] = LanguageFileParser
}
