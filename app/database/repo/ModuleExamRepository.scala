package database.repo

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import database.table.ModuleExamTable
import database.table.ModuleTable
import models.ModuleExams
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.TransactionIsolation

@Singleton
final class ModuleExamRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api.*

  private val tableQuery = TableQuery[ModuleExamTable]

  private def moduleQuery(module: UUID) = TableQuery[ModuleTable].filter(_.id === module)

  /**
   * Returns the title and exams of the live module, or `None` if it does not exist. Child and generic modules have no
   * own exams and yield an empty list.
   */
  def get(module: UUID): Future[Option[(String, ModuleExams)]] = {
    val action = moduleQuery(module).map(m => (m.title, m.hasOwnExams)).result.headOption.flatMap {
      case Some((title, true)) =>
        tableQuery
          .filter(a => a.module === module && a.name.isDefined)
          .sortBy(_.name)
          .map(_.name.get)
          .result
          .map(names => Some((title, ModuleExams(Option.when(names.nonEmpty)(names.toList)))))
      case row => DBIO.successful(row.map((title, _) => (title, ModuleExams(Some(Nil)))))
    }
    db.run(action.transactionally.withTransactionIsolation(TransactionIsolation.RepeatableRead))
  }

  /**
   * Replaces the exams of the live module atomically. The module row is locked first so that concurrent updates,
   * module syncs and deletions are serialized and the eligibility check sees their committed state.
   *
   * @return `None` if the module does not exist, `Some(false)` if it has no own exams
   */
  def replace(module: UUID, names: Option[List[String]]): Future[Option[Boolean]] = {
    val action = for {
      _    <- moduleQuery(module).map(_.id).forUpdate.result
      owns <- moduleQuery(module).map(_.hasOwnExams).result.headOption
      _    <-
        if owns.contains(true) then
          tableQuery.filter(_.module === module).delete >>
            (tableQuery ++= names.fold(List(module -> None))(_.map(module -> Some(_))))
        else DBIO.unit
    } yield owns
    db.run(action.transactionally)
  }
}
