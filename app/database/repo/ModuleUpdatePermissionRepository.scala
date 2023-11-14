package database.repo

import database.table.{
  ModuleCompendiumTable,
  ModuleDraftTable,
  ModuleUpdatePermissionTable
}
import models.ModuleUpdatePermissionType.Inherited
import models.{CampusId, ModuleUpdatePermissionType}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleUpdatePermissionRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[
      (UUID, CampusId, ModuleUpdatePermissionType),
      (UUID, CampusId, ModuleUpdatePermissionType),
      ModuleUpdatePermissionTable
    ]
    with HasDatabaseConfigProvider[JdbcProfile] {

  import database.table.{
    campusIdColumnType,
    jsValueColumnType,
    moduleUpdatePermissionTypeColumnType
  }
  import profile.api._

  protected val tableQuery = TableQuery[ModuleUpdatePermissionTable]

  override protected def retrieve(
      query: Query[
        ModuleUpdatePermissionTable,
        (UUID, CampusId, ModuleUpdatePermissionType),
        Seq
      ]
  ) =
    db.run(query.result)

  def deleteByModules(
      modules: Seq[UUID],
      kind: ModuleUpdatePermissionType
  ): Future[Int] =
    db.run(
      tableQuery
        .filter(e => e.kind === kind && e.module.inSet(modules))
        .delete
    )

  def delete(
      module: UUID,
      campusId: CampusId,
      kind: ModuleUpdatePermissionType
  ): Future[Int] =
    db.run(
      tableQuery
        .filter(a =>
          a.module === module && a.campusId === campusId && a.kind === kind
        )
        .delete
    )

  def allWithModule(campusId: CampusId) =
    db.run(
      tableQuery
        .filter(_.campusId === campusId)
        .joinLeft(
          TableQuery[ModuleCompendiumTable].map(a => (a.id, a.title, a.abbrev))
        )
        .on(_.module === _._1)
        .joinLeft(TableQuery[ModuleDraftTable].map(a => (a.module, a.data)))
        .on(_._1.module === _._1)
        .result
        .map(_.map { case (((id, campusId, kind), m), d) =>
          val module = m
            .map(a => Left(a._2, a._3))
            .orElse(d.map(a => Right(a._2)))
            .get // either of them must be defined
          (id, campusId, kind, module)
        })
    )

  def hasPermission(campusId: CampusId, module: UUID): Future[Boolean] =
    db.run(
      tableQuery
        .filter(a => a.module === module && a.campusId === campusId)
        .exists
        .result
    )

  def hasInheritedPermission(
      campusId: CampusId,
      module: UUID
  ): Future[Boolean] = {
    val inherited: ModuleUpdatePermissionType = Inherited
    db.run(
      tableQuery
        .filter(a =>
          a.module === module && a.campusId === campusId && a.kind === inherited
        )
        .exists
        .result
    )
  }
}
