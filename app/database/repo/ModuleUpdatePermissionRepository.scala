package database.repo

import database.Filterable
import database.table.{ModuleCompendiumTable, ModuleUpdatePermissionTable}
import models.ModuleUpdatePermissionType.Inherited
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

object ModuleUpdatePermissionRepository {
  val campusIdFilter = "campusId"
  val moduleFilter = "module"
}

@Singleton
final class ModuleUpdatePermissionRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends Repository[
      (UUID, CampusId, ModuleUpdatePermissionType),
      (UUID, CampusId, ModuleUpdatePermissionType),
      ModuleUpdatePermissionTable
    ]
    with HasDatabaseConfigProvider[JdbcProfile]
    with Filterable[
      (UUID, CampusId, ModuleUpdatePermissionType),
      ModuleUpdatePermissionTable
    ] {

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

  def allWithModule(filter: Filter) =
    db.run(
      allWithFilter(filter)
        .join(
          TableQuery[ModuleCompendiumTable].map(a => (a.id, a.title, a.abbrev))
        )
        .on(_.module === _._1)
        .result
        .map(_.map { case ((id, campusId, kind), (_, title, abbrev)) =>
          ModuleUpdatePermission(id, title, abbrev, campusId, kind)
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

  def allForCampusId(
      campusId: CampusId
  ): Future[Seq[(ModuleUpdatePermissionType, Module)]] = {
    val query = for {
      q <- tableQuery if q.campusId === campusId
      m <- q.moduleFk
    } yield (q.kind, (m.id, m.title, m.abbrev))
    db.run(query.result.map(_.map(a => (a._1, Module.tupled(a._2)))))
  }

  override protected val makeFilter: PartialFunction[(String, String), Pred] = {
    case (ModuleUpdatePermissionRepository.campusIdFilter, value) =>
      _.campusId === CampusId(value)
    case (ModuleUpdatePermissionRepository.moduleFilter, value) =>
      _.module === UUID.fromString(value)
  }
}
