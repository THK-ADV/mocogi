package service.core

import database.repo.LanguageRepository
import models.core.Language
import parsing.core.LanguageFileParser

import javax.inject.{Inject, Singleton}

trait LanguageService extends SimpleYamlService[Language]

@Singleton
final class LanguageServiceImpl @Inject() (
    val repo: LanguageRepository,
    val parser: LanguageFileParser
) extends LanguageService
