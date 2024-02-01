package service.core

import database.repo.LanguageRepository
import models.core.Language
import parsing.core.{FileParser, LanguageFileParser}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

trait LanguageService extends SimpleYamlService[Language]

@Singleton
final class LanguageServiceImpl @Inject() (
    val repo: LanguageRepository,
    val ctx: ExecutionContext
) extends LanguageService {
  override def fileParser: FileParser[Language] = LanguageFileParser
}
