package database.repo

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.CampusId
import database.table.core.IdentityTable
import database.table.ModuleDraftTable
import database.table.ModuleUpdatePermissionTable
import models.*
import models.core.Identity
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.GetResult
import slick.jdbc.JdbcProfile
import slick.jdbc.TypedParameter

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

  def allPeopleWithPermissionForModule(module: UUID): Future[Seq[(Identity.Person, ModuleUpdatePermissionType)]] =
    db.run(
      tableQuery
        .filter(_.module === module)
        .join(TableQuery[IdentityTable].filter(_.isPerson))
        .on(_.campusId.value.asColumnOf[String] === _.campusId)
        .map(a => (a._2, a._1.kind))
        .distinctOn(_._1.id)
        .result
        .map(_.map { case (id, perm) => (Identity.toPersonUnsafe(id), perm) })
    )

  def allGrantedFromModule(module: UUID) = {
    val query =
      sql"select modules.get_users_with_granted_permissions_from_module(${module.toString}::uuid)".as[String].head
    db.run(query)
  }

  def hasPermission(campusId: CampusId, module: UUID): Future[Boolean] =
    db.run(
      tableQuery
        .filter(a => a.module === module && a.campusId === campusId)
        .exists
        .result
    )

  def isAuthorOf(moduleId: UUID, personId: String) =
    db.run(
      TableQuery[ModuleDraftTable]
        .filter(a => a.module === moduleId && a.author === personId)
        .exists
        .result
    )

  private def arrayLiteral(pos: Iterable[String]) =
    "'{" + pos.mkString(",") + "}'"

  def allForUser(cid: CampusId): Future[String] = {
    val query = sql"select modules.get_modules_for_user(${cid.value}::text)".as[String].head
    db.run(query)
  }

  def allForPos(pos: Set[String]): Future[String] = {
    val query = sql"select modules.get_modules_for_po(#${arrayLiteral(pos)}::text[])".as[String].head
    db.run(query)
  }

  // Checks if the module has a PO relationship with any of the passed POs. Both live and draft modules are considered
  def isModulePartOfPO(module: UUID, pos: Set[String]): Future[Boolean] = {
    val query =
      sql"select modules.module_of_po(${module.toString}::uuid, #${arrayLiteral(pos)}::text[])".as[Boolean].head
    db.run(query)
  }
}
