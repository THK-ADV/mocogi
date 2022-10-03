package service

import basedata.Person
import database.repo.PersonRepository
import parsing.base.PersonFileParser

import javax.inject.{Inject, Singleton}

trait PersonService extends YamlService[Person]

@Singleton
final class PersonServiceImpl @Inject() (
    val repo: PersonRepository,
    val parser: PersonFileParser
) extends PersonService
