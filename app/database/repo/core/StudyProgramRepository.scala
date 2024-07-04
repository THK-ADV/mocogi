package database.repo.core

import cats.data.NonEmptyList
import database.table.core.{
  StudyProgramDbEntry,
  StudyProgramPersonDbEntry,
  StudyProgramPersonTable,
  StudyProgramTable
}
import models.UniversityRole
import models.core.{IDLabel, StudyProgram}
import play.api.Logging
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StudyProgramRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with Logging {
  import profile.api._

  protected val tableQuery = TableQuery[StudyProgramTable]

  protected val personAssocQuery =
    TableQuery[StudyProgramPersonTable]

  def all(): Future[Seq[StudyProgram]] =
    retrieve(tableQuery)

  def allIds(): Future[Seq[String]] =
    db.run(tableQuery.map(_.id).result)

  def createOrUpdateMany(
      xs: Seq[StudyProgram]
  ): Future[Seq[StudyProgram]] = {
    def directors(x: StudyProgram) = {
      val directors = ListBuffer.empty[StudyProgramPersonDbEntry]
      x.programDirectors.map(p =>
        directors += StudyProgramPersonDbEntry(p, x.id, UniversityRole.SGL)
      )
      x.examDirectors.map(p =>
        directors += StudyProgramPersonDbEntry(p, x.id, UniversityRole.PAV)
      )
      directors.toList
    }

    def update(x: StudyProgram) =
      for {
        _ <- personAssocQuery.filter(_.studyProgram === x.id).delete
        _ <- tableQuery.filter(_.id === x.id).update(toDbEntry(x))
        _ <- personAssocQuery ++= directors(x)
      } yield ()

    def create(x: StudyProgram) =
      for {
        _ <- tableQuery += toDbEntry(x)
        _ <- personAssocQuery ++= directors(x)
      } yield ()

    db.run(
      DBIO
        .sequence(
          xs.map { x =>
            for {
              exists <- tableQuery.filter(_.id === x.id).exists.result
              res <- if (exists) update(x) else create(x)
            } yield res
          }
        )
        .transactionally
        .map(_ => xs)
    )
  }

  def allWithDegrees() = {
    val query = for {
      q <- tableQuery
      d <- q.degreeFk
    } yield (q.id, q.deLabel, q.enLabel, d)
    db.run(query.result.map(_.map(a => (IDLabel(a._1, a._2, a._3), a._4))))
  }

  private def retrieve(
      query: Query[StudyProgramTable, StudyProgramDbEntry, Seq]
  ) =
    db.run(
      query
        .joinLeft(personAssocQuery)
        .on(_.id === _.studyProgram)
        .result
        .map(_.groupBy(_._1.id).map { case (_, xs) =>
          val directors = mutable.HashSet[String]()
          val examDirectors = mutable.HashSet[String]()
          val sp = xs.head._1
          xs.foreach {
            case (_, Some(sgl)) if sgl.role == UniversityRole.SGL =>
              directors += sgl.person
            case (_, Some(pav)) if pav.role == UniversityRole.PAV =>
              examDirectors += pav.person
            case x =>
              logger.error(
                s"found a missing case while retrieving directors of a study program: $x"
              )
          }
          StudyProgram(
            sp.id,
            sp.deLabel,
            sp.enLabel,
            sp.internalAbbreviation,
            sp.externalAbbreviation,
            sp.degree,
            NonEmptyList.fromListUnsafe(directors.toList),
            NonEmptyList.fromListUnsafe(examDirectors.toList)
          )
        }.toSeq)
    )

  private def toDbEntry(sp: StudyProgram): StudyProgramDbEntry =
    StudyProgramDbEntry(
      sp.id,
      sp.deLabel,
      sp.enLabel,
      sp.internalAbbreviation,
      sp.externalAbbreviation,
      sp.degree
    )
}
