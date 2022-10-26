package service

import basedata.Faculty
import database.repo.FacultyRepository
import parsing.base.FacultyFileParser

import javax.inject.{Inject, Singleton}

trait FacultyService extends SimpleYamlService[Faculty]

@Singleton
final class FacultyServiceImpl @Inject() (
    val repo: FacultyRepository
) extends FacultyService {
  override def parser = FacultyFileParser
}
