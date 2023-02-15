package service.core

import database.repo.FacultyRepository
import models.core.Faculty
import parsing.core.FacultyFileParser

import javax.inject.{Inject, Singleton}

trait FacultyService extends SimpleYamlService[Faculty]

@Singleton
final class FacultyServiceImpl @Inject() (
    val repo: FacultyRepository
) extends FacultyService {
  override def parser = FacultyFileParser
}
