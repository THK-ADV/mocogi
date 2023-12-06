package database.repo

import database.table.{ModuleCompendiumTable, ModuleUpdatePermissionTable}
import models.{
  CampusId,
  Module,
  ModuleUpdatePermission,
  ModuleUpdatePermissionType
}
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
      campusIds: List[CampusId],
      kind: ModuleUpdatePermissionType
  ): Future[Int] =
    db.run(
      tableQuery
        .filter(a =>
          a.module === module && a.campusId.inSet(campusIds) && a.kind === kind
        )
        .delete
    )

  def allFromUser(campusId: CampusId) =
    db.run(
      tableQuery
        .filter(_.campusId === campusId)
        .join(
          TableQuery[ModuleCompendiumTable].map(a => (a.id, a.title, a.abbrev))
        )
        .on(_.module === _._1)
        .result
        .map(_.map { case ((id, campusId, kind), (_, title, abbrev)) =>
          ModuleUpdatePermission(id, title, abbrev, campusId, kind)
        })
    )

  def allGrantedFromModule(module: UUID) =
    db.run(
      tableQuery
        .filter(a => a.module === module && a.isGranted)
        .map(_.campusId)
        .result
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
  ): Future[Boolean] =
    db.run(
      tableQuery
        .filter(a =>
          a.module === module && a.campusId === campusId && a.isInherited
        )
        .exists
        .result
    )

  def allForCampusId(
      campusId: CampusId
  ): Future[Seq[(ModuleUpdatePermissionType, Module)]] = {
    val query = for {
      q <- tableQuery if q.campusId === campusId
      m <- q.moduleFk
    } yield (q.kind, (m.id, m.title, m.abbrev))
    db.run(query.result.map(_.map(a => (a._1, Module.tupled(a._2)))))
  }
}
