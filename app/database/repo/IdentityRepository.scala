package database.repo

import database.table._
import models.CampusId
import models.core.Identity
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IdentityRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[IdentityDbEntry, Identity, IdentityTable]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[IdentityTable]

  private val personInFacultyTableQuery = TableQuery[PersonInFacultyTable]

  private val facultyTableQuery = TableQuery[FacultyTable]

  override def createOrUpdateMany(ls: Seq[IdentityDbEntry]) = {
    val action = for {
      _ <- DBIO.sequence(ls.map(tableQuery.insertOrUpdate))
      _ <- personInFacultyTableQuery.delete
      _ <- personInFacultyTableQuery ++= ls.flatMap(p =>
        p.faculties.map(f => PersonInFaculty(p.id, f))
      )
    } yield ls
    db.run(action.transactionally)
  }

  override def createMany(ls: Seq[IdentityDbEntry]) = {
    val action = for {
      _ <- tableQuery ++= ls
      _ <- personInFacultyTableQuery ++= ls.flatMap(p =>
        p.faculties.map(f => PersonInFaculty(p.id, f))
      )
    } yield ls
    db.run(action.transactionally)
  }

  override protected def retrieve(
      query: Query[IdentityTable, IdentityDbEntry, Seq]
  ) =
    db.run(
      query
        .joinLeft(personInFacultyTableQuery)
        .on(_.id === _.person)
        .joinLeft(facultyTableQuery)
        .on((a, b) => a._2.map(_.faculty === b.id).getOrElse(false))
        .result
        .map(_.groupBy(_._1._1).map { case (person, deps) =>
          val faculties = deps.flatMap(_._2)
          Identity.fromDbEntry(person, faculties.toList)
        }.toSeq)
    )

  def getByCampusId(campusId: CampusId): Future[Option[Identity.Person]] =
    db.run(
      tableQuery
        .filter(a =>
          a.campusId === campusId.value && a.kind === Identity.DefaultKind
        )
        .result
        .map(p =>
          Option.when(p.size == 1) {
            val p0 = p.head
            Identity.Person(
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
