package database.repo

import database.table.{
  ModuleCompendiumTable,
  POOptionalTable,
  POTable,
  IdentityTable,
  ResponsibilityTable,
  SpecializationTable
}
import models.core.Identity
import models.{Module, POShort}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
final class WPFRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    private implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val tableQuery = TableQuery[ModuleCompendiumTable]

  def all() = {
    val poQuery = for {
      q <- TableQuery[POTable] if q.isValid()
      sp <- q.studyProgramFk
      g <- sp.gradeFk
    } yield (q.id, q.version, (sp.id, sp.deLabel, sp.enLabel, g))

    db.run(
      tableQuery
        .join(TableQuery[ResponsibilityTable].filter(_.isModuleManager))
        .on(_.id === _.metadata)
        .join(TableQuery[IdentityTable])
        .on(_._2.identity === _.id)
        .join(TableQuery[POOptionalTable])
        .on(_._1._1.id === _.metadata)
        .join(poQuery)
        .on(_._2.po === _._1)
        .joinLeft(TableQuery[SpecializationTable])
        .on(_._1._2.specialization === _.id)
        .map { case (((((mc, _), p), _), po), poSpec) =>
          (mc.id, mc.title, mc.abbrev, p, po, poSpec)
        }
        .result
        .map(_.groupBy(_._1).collect {
          case (_, xs) if xs.nonEmpty =>
            val m = Module(xs.head._1, xs.head._2, xs.head._3)
            val p = Identity.fromDbEntry(xs.head._4, Nil)
            val pos = xs.map(a => POShort(a._5, a._6))
            (m, p, pos)
        })
    )
  }
}
