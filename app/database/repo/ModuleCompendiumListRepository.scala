package database.repo

import database.table.ModuleCompendiumListTable
import models.{ModuleCompendiumList, Semester, StudyProgramShort}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleCompendiumListRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[
      ModuleCompendiumList.DB,
      ModuleCompendiumList.DB,
      ModuleCompendiumListTable
    ]
    with HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  protected val tableQuery = TableQuery[ModuleCompendiumListTable]

  override protected def retrieve(
      query: Query[ModuleCompendiumListTable, ModuleCompendiumList.DB, Seq]
  ) = db.run(query.result)

  private def retrieveAtomic(
      query: Query[ModuleCompendiumListTable, ModuleCompendiumList.DB, Seq]
  ): Future[Seq[ModuleCompendiumList.Atomic]] = {
    val q = for {
      q <- query
      sp <- q.studyProgramFk
      g <- sp.gradeFk
    } yield (q, (sp.abbrev, sp.deLabel, sp.enLabel), g)

    db.run(
      q.result.map(_.map { case (mcl, sp, g) =>
        mcl.copy(
          studyProgram = StudyProgramShort(sp._1, sp._2, sp._3, g),
          semester = Semester(mcl.semester)
        )
      })
    )
  }

  def allFromSemester(semester: String) =
    retrieveAtomic(tableQuery.filter(_.semester === semester))
}
