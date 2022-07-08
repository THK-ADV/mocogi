package service

import database.repo.LanguageRepository
import parsing.metadata.file.LanguageFileParser
import parsing.types.Language

import javax.inject.{Inject, Singleton}

trait LanguageService extends YamlService[Language]

@Singleton
final class LanguageServiceImpl @Inject() (
    val repo: LanguageRepository,
    val parser: LanguageFileParser
) extends LanguageService
