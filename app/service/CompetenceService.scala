package service

import basedata.Competence
import database.repo.CompetenceRepository
import parsing.base.CompetenceFileParser

import javax.inject.{Inject, Singleton}

trait CompetenceService extends SimpleYamlService[Competence]

@Singleton
final class CompetenceServiceImpl @Inject() (
    val repo: CompetenceRepository,
    val parser: CompetenceFileParser
) extends CompetenceService
