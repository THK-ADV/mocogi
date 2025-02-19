package database.repo

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.CampusId
import database.table.CreatedModuleTable
import database.table.ModuleDraftTable
import database.table.ModuleTable
import database.table.ModuleUpdatePermissionTable
import models.*
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

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

  import database.table.campusIdColumnType
  import database.table.moduleUpdatePermissionTypeColumnType
  import profile.api.*

  protected val tableQuery = TableQuery[ModuleUpdatePermissionTable]

  protected override def retrieve(
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
        .filter(a => a.module === module && a.campusId.inSet(campusIds) && a.kind === kind)
        .delete
    )

  def delete(
      module: UUID,
      campusIds: Seq[CampusId]
  ): Future[Int] =
    db.run(
      tableQuery
        .filter(a => a.module === module && a.campusId.inSet(campusIds))
        .delete
    )

  def deleteInherited(module: UUID) =
    db.run(tableQuery.filter(a => a.module === module && a.isInherited).delete)

  def deleteGranted(module: UUID) =
    db.run(tableQuery.filter(a => a.module === module && a.isGranted).delete)

  def allFromUser(campusId: CampusId) =
    db.run(
      tableQuery
        .filter(_.campusId === campusId)
        .join(
          TableQuery[ModuleTable].map(a => (a.id, a.title, a.abbrev))
        )
        .on(_.module === _._1)
        .result
        .map(_.map {
          case ((id, campusId, kind), (_, title, abbrev)) =>
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
        .filter(a => a.module === module && a.campusId === campusId && a.isInherited)
        .exists
        .result
    )

  def allForCampusId(
      campusId: CampusId
  ): Future[
    Seq[(ModuleCore, ModuleUpdatePermissionType, Option[ModuleDraft])]
  ] = {
    val permissions = tableQuery.filter(_.campusId === campusId)
    val liveTable   = TableQuery[ModuleTable].map(a => (a.id, a.title, a.abbrev))
    val draftTable  = TableQuery[CreatedModuleTable].map(a => (a.module, a.moduleTitle, a.moduleAbbrev))

    val q1 = permissions
      .joinLeft(liveTable)
      .on(_.module === _._1)
      .joinLeft(TableQuery[ModuleDraftTable])
      .on(_._1.module === _.module)
      .filter(a => a._1._2.isDefined || a._2.isDefined)
    val q2 = permissions
      .joinLeft(draftTable)
      .on(_.module === _._1)
      .joinLeft(TableQuery[ModuleDraftTable])
      .on(_._1.module === _.module)
      // I am failing to project a constant null column for module draft. One module should only exist in either live or draft table
      .filter(a => a._1._2.isDefined && a._2.isEmpty)

    db.run(
      q1.unionAll(q2)
        .result
        .map { xs =>
          val res = xs.map {
            case (((_, _, kind), core), draft) =>
              val moduleCore = core
                .map(ModuleCore.apply.tupled)
                .orElse(draft.map(d => ModuleCore(d.module, d.moduleTitle, d.moduleAbbrev)))
                .get
              (moduleCore, kind, draft)
          }
          assume(xs.size == res.distinctBy(_._1.id).size, s"expected unique modules, but found: $res")
          res
        }
    )
  }
}
