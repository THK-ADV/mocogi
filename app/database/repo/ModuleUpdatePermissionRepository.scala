package database.repo

import database.table.ModuleUpdatePermissionTable
import models.{ModuleUpdatePermissionType, User}
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
      (UUID, User, ModuleUpdatePermissionType),
      (UUID, User, ModuleUpdatePermissionType),
      ModuleUpdatePermissionTable
    ]
    with HasDatabaseConfigProvider[JdbcProfile] {

  import database.table.userColumnType
  import profile.api._

  protected val tableQuery = TableQuery[ModuleUpdatePermissionTable]

  override protected def retrieve(
      query: Query[
        ModuleUpdatePermissionTable,
        (UUID, User, ModuleUpdatePermissionType),
        Seq
      ]
  ) =
    db.run(query.result)

  def deleteByModules(modules: Seq[UUID], kind: ModuleUpdatePermissionType) =
    db.run(
      tableQuery
        .filter(e => e.kind === kind.value && e.module.inSet(modules))
        .delete
    )

  def delete(
      module: UUID,
      user: User,
      kind: ModuleUpdatePermissionType
  ) =
    db.run(
      tableQuery
        .filter(a =>
          a.module === module && a.user === user && a.kind === kind.value
        )
        .delete
    )

  def allWithModule() =
    db.run(
      (for {
        q <- tableQuery
        m <- q.moduleFk
      } yield (m.id, m.title, m.abbrev, q.user, q.kind)).result
    )

  def hasPermission(user: User, module: UUID): Future[Boolean] =
    db.run(
      tableQuery
        .filter(a => a.module === module && a.user === user)
        .exists
        .result
    )
}