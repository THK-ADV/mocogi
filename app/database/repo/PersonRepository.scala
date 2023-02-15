package database.repo

import database.table._
import models.core.{Faculty, Person}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PersonRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[PersonDbEntry, Person, PersonTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[PersonTable]

  private val personInFacultyTableQuery = TableQuery[PersonInFacultyTable]

  private val facultyTableQuery = TableQuery[FacultyTable]

  override def createMany(ls: List[PersonDbEntry]) = {
    val action = for {
      _ <- tableQuery ++= ls
      _ <- personInFacultyTableQuery ++= ls.flatMap(p =>
        p.faculties.map(f => PersonInFaculty(p.id, f))
      )
    } yield ls
    db.run(action.transactionally)
  }

  override protected def retrieve(
      query: Query[PersonTable, PersonDbEntry, Seq]
  ) =
    db.run(
      tableQuery
        .joinLeft(personInFacultyTableQuery)
        .on(_.id === _.person)
        .joinLeft(facultyTableQuery)
        .on((a, b) => a._2.map(_.faculty === b.abbrev).getOrElse(false))
        .result
        .map(_.groupBy(_._1._1).map { case (person, deps) =>
          val faculties = deps.flatMap(_._2)
          toPerson(person, faculties.toList)
        }.toSeq)
    )

  private def toPerson(p: PersonDbEntry, faculties: List[Faculty]): Person =
    p.kind match {
      case Person.SingleKind =>
        Person.Single(
          p.id,
          p.lastname,
          p.firstname,
          p.title,
          faculties,
          p.abbreviation,
          p.status
        )
      case Person.GroupKind =>
        Person.Group(p.id, p.title)
      case Person.UnknownKind =>
        Person.Unknown(p.id, p.title)
    }
}
