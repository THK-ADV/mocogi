package service.core

import database.repo.CompetenceRepository
import models.core.Competence
import parsing.core.CompetenceFileParser

import javax.inject.{Inject, Singleton}

trait CompetenceService extends SimpleYamlService[Competence]

@Singleton
final class CompetenceServiceImpl @Inject() (
    val repo: CompetenceRepository,
    val parser: CompetenceFileParser
) extends CompetenceService
