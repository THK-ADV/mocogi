package database.repo

import auth.CampusId
import database.table.{ModuleDraftTable, ModuleTable, ModuleUpdatePermissionTable}
import database.table.core.IdentityTable
import models.*
import models.core.Identity
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.{GetResult, JdbcProfile, TypedParameter}

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

  import database.table.{campusIdColumnType, moduleUpdatePermissionTypeColumnType}
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
    val query = sql"select get_users_with_granted_permissions_from_module(${module.toString}::uuid)".as[String].head
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

  private given GetResult[String] =
    GetResult(_.nextString())

  private def arrayLiteral(pos: Seq[String]) =
    "'{" + pos.mkString(",") + "}'"

  def allForUser(cid: CampusId): Future[String] = {
    val query = sql"select get_modules_for_user(${cid.value}::text)".as[String].head
    db.run(query)
  }

  // This function is only used for accreditation members which can access all the modules for a given PO
  def allForPos(pos: Seq[String]): Future[String] = {
    val query = sql"select get_modules_for_po(#${arrayLiteral(pos)}::text[])".as[String].head
    db.run(query)
  }

  // Checks if the module has a PO relationship with any of the passed POs. Both live and draft modules are considered
  def isModulePartOfPO(module: UUID, pos: Seq[String]): Future[Boolean] = {
    val query = sql"select module_of_po(${module.toString}::uuid, #${arrayLiteral(pos)}::text[])".as[Boolean].head
    db.run(query)
  }
}
