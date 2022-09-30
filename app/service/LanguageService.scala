package service

import basedata.Language
import database.repo.LanguageRepository
import parsing.base.LanguageFileParser

import javax.inject.{Inject, Singleton}

trait LanguageService extends YamlService[Language]

@Singleton
final class LanguageServiceImpl @Inject() (
    val repo: LanguageRepository,
    val parser: LanguageFileParser
) extends LanguageService
