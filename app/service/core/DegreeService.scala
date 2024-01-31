package service.core

import database.repo.DegreeRepository
import models.core.Degree
import parsing.core.DegreeFileParser

import javax.inject.{Inject, Singleton}

trait DegreeService extends SimpleYamlService[Degree]

@Singleton
final class DegreeServiceImpl @Inject() (
    val repo: DegreeRepository
) extends DegreeService {
  override def parser = DegreeFileParser
}
