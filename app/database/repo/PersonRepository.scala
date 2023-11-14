package database.repo

import database.table._
import models.CampusId
import models.core.Person
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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

  override def createMany(ls: Seq[PersonDbEntry]) = {
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
          Person.fromDbEntry(person, faculties.toList)
        }.toSeq)
    )

  def getByCampusId(campusId: CampusId): Future[Option[Person.Default]] =
    db.run(
      tableQuery
        .filter(a =>
          a.campusId === campusId.value && a.kind === Person.DefaultKind
        )
        .result
        .map(p =>
          Option.when(p.size == 1) {
            val p0 = p.head
            Person.Default(
              p0.id,
              p0.lastname,
              p0.firstname,
              p0.title,
              Nil,
              p0.abbreviation,
              p0.campusId.get,
              p0.status
            )
          }
        )
    )
}
