package database.repo.core

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.data.NonEmptyList
import database.table.core.StudyProgramDbEntry
import database.table.core.StudyProgramPersonDbEntry
import database.table.core.StudyProgramPersonTable
import database.table.core.StudyProgramTable
import models.core.StudyProgram
import models.UniversityRole
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
class StudyProgramRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with CrudRepository[StudyProgram] {
  import profile.api.*

  protected val tableQuery = TableQuery[StudyProgramTable]

  private val personAssocQuery = TableQuery[StudyProgramPersonTable]

  def list(): Future[Seq[StudyProgram]] =
    db.run(tableQuery.joinLeft(personAssocQuery).on(_.id === _.studyProgram).result).map {
      _.groupBy(_._1.id).values
        .map { rows =>
          val sp        = rows.head._1
          val directors = rows.flatMap(_._2)
          StudyProgram(
            sp.id,
            sp.deLabel,
            sp.enLabel,
            sp.abbreviation,
            sp.degree,
            NonEmptyList.fromListUnsafe(directors.filter(_.role == UniversityRole.SGL).map(_.person).toList.distinct),
            NonEmptyList.fromListUnsafe(directors.filter(_.role == UniversityRole.PAV).map(_.person).toList.distinct)
          )
        }
        .toSeq
    }

  def create(input: StudyProgram): Future[StudyProgram] =
    db.run(save(input)(tableQuery += toDbEntry(input)).transactionally).map(_ => input)

  def update(id: String, input: StudyProgram): Future[Int] =
    db.run(save(input)(tableQuery.filter(_.id === id).update(toDbEntry(input))).transactionally)

  private def save(input: StudyProgram)(write: DBIO[Int]): DBIO[Int] =
    for {
      n <- write
      _ <-
        if n == 0 then DBIO.successful(())
        else {
          val directors =
            input.programDirectors.toList.map(StudyProgramPersonDbEntry(_, input.id, UniversityRole.SGL)) ++
              input.examDirectors.toList.map(StudyProgramPersonDbEntry(_, input.id, UniversityRole.PAV))
          for {
            _ <- personAssocQuery.filter(_.studyProgram === input.id).delete
            _ <- personAssocQuery ++= directors.distinct
          } yield ()
        }
    } yield n

  private def toDbEntry(sp: StudyProgram): StudyProgramDbEntry =
    StudyProgramDbEntry(
      sp.id,
      sp.deLabel,
      sp.enLabel,
      sp.abbreviation,
      sp.degree
    )
}
