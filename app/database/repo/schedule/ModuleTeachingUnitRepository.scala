package database.repo.schedule

import database.table.core.TeachingUnitTable
import database.table.schedule.ModuleTeachingUnitTable
import models.schedule.ModuleTeachingUnit
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleTeachingUnitRepository @Inject()(
  val dbConfigProvider: DatabaseConfigProvider,
  implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api.*

  /**
   * Recreates the mapping between modules and teaching units in the database.
   * UUID is the module ID. List[String] is the PO-ID.
   */
  def recreate(modules: Seq[(UUID, List[String])]): Future[Unit] =
    for {
      teachingUnits <- db.run(TableQuery[TeachingUnitTable].result)
      entries = {
        val inf = teachingUnits.find(_.isINF).get.id
        val ing = teachingUnits.find(_.isING).get.id
        modules.map { (module, pos) =>
          val tus = mutable.Set.empty[UUID]
          for (po <- pos) {
            if po.startsWith("inf") then tus.add(inf)
            if po.startsWith("ing") then tus.add(ing)
          }
          ModuleTeachingUnit(module, tus.toList)
        }
      }
      recreate <- db.run(
        DBIO.seq(
          TableQuery[ModuleTeachingUnitTable].delete,
          TableQuery[ModuleTeachingUnitTable].insertAll(entries),
        ).transactionally
      )
    } yield ()
}
