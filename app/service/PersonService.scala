package service

import database.repo.PersonRepository
import parsing.metadata.file.PersonFileParser
import parsing.types.Person

import javax.inject.{Inject, Singleton}

trait PersonService extends YamlService[Person]

@Singleton
final class PersonServiceImpl @Inject() (
    val repo: PersonRepository,
    val parser: PersonFileParser
) extends PersonService
