package database.repo

import javax.inject.Inject
import javax.inject.Singleton

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import auth.CampusId
import database.table.core.IdentityTable
import database.table.core.POTable
import database.table.core.StudyProgramPersonTable
import database.table.Permission
import database.table.PermissionTable
import models.core.Identity
import models.UniversityRole
import permission.PermissionType
import permission.Permissions
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class PermissionRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {
  import database.table.universityRoleColumnType
  import profile.api.*

  private type POs        = Set[String]
  private type Permission = (PermissionType, POs)

  private def tableQuery              = TableQuery[PermissionTable]
  private def identityQuery           = TableQuery[IdentityTable]
  private def studyProgramPersonQuery = TableQuery[StudyProgramPersonTable]

  private def poTableQuery = TableQuery[POTable].filter(p => !p.isExpired())

  // Returns active person for the ID
  private def getPerson(campusId: CampusId): Future[Option[Identity.Person]] =
    db.run(
      identityQuery
        .filter(a => a.isActive && a.campusId === campusId.value && a.isPerson)
        .result
        .map(p => Option.when(p.size == 1)(Identity.toPersonUnsafe(p.head)))
    )

  // Convert granted PAV permissions to module permissions of non-expired POs if the person is a PAV
  private def getPAVModulePermissions(person: String): Future[Option[Permission]] =
    db.run(
      studyProgramPersonQuery
        .filter(s => s.isPAV && s.person === person)
        .join(poTableQuery)
        .on(_.studyProgram === _.studyProgram)
        .map(_._2.id)
        .distinct
        .result
        .map(pos => if (pos.isEmpty) None else Some((PermissionType.Module, Set(pos*))))
    )

  // Convert granted PAV or SGL permissions to artifact permissions of non-expired POs
  private def getArtifactPermissions(person: String): Future[Option[Seq[Permission]]] = {
    def collect(xs: Seq[(UniversityRole, String)]): Seq[Permission] = {
      val map = scala.collection.mutable.Map.empty[PermissionType, POs]

      def update(key: PermissionType, value: String) =
        map.updateWith(key) {
          case Some(existingSet) => Some(existingSet + value)
          case None              => Some(Set(value))
        }

      for ((role, po) <- xs) yield {
        role match {
          case UniversityRole.SGL =>
            // SGL role is lifted to ArtifactsPreview
            update(PermissionType.ArtifactsPreview, po)
          case UniversityRole.PAV =>
            // PAV role is lifted to ArtifactsCreate
            update(PermissionType.ArtifactsCreate, po)
        }
      }
      map.toSeq
    }

    db.run(
      studyProgramPersonQuery
        .filter(_.person === person)
        .join(poTableQuery)
        .on(_.studyProgram === _.studyProgram)
        .map((a, b) => (a.role, b.id))
        .distinct
        .result
        .map(xs => if xs.isEmpty then None else Some(collect(xs)))
    )
  }

  private def isTeachingUnit(str: String): Boolean =
    !str.contains('_') && (str == "inf" || str == "ing")

  // Returns one permission object with all non-expired POs if the person is an admin.
  // Otherwise, all permissions are returned with their POs properly resolved
  private def getAllPermissions(person: String): Future[Seq[Permission] | Permission] =
    for {
      dbPerms: Seq[database.table.Permission] <- db.run(tableQuery.filter(_.person === person).result)
      perms                                   <- dbPerms.find(_.permType.isAdmin) match {
        case Some(Permission(_, permType, _, _)) =>
          // an admin role is combined into one which can perform all actions on all non-expired POs
          db.run(poTableQuery.map(_.id).result.map(pos => (permType, Set(pos*))))
        case None =>
          // resolve each context object to specific POs
          Future.sequence(dbPerms.map { (p: database.table.Permission) =>
            p.context match {
              case Some(context) =>
                // teaching units are resolved to their POs
                val (teachingUnits, pos) = context.partition(isTeachingUnit)
                if teachingUnits.isEmpty then {
                  Future.successful((p.permType, Set(context*)))
                } else {
                  val query = poTableQuery.filter { po =>
                    var acc: Rep[Boolean] = false
                    for tu <- teachingUnits yield {
                      acc = acc || po.id.startsWith(tu)
                    }
                    acc
                  }
                  db.run(query.map(_.id).result).map(tuPOs => (p.permType, pos.appendedAll(tuPOs).toSet))
                }
              case None =>
                Future.successful((p.permType, Set.empty))
            }
          })
      }
    } yield perms

  /**
   * Retrieves all permissions for the given campus ID, along with the associated person if available.
   */
  def all(campusId: CampusId): Future[Option[(Identity.Person, Permissions)]] =
    getPerson(campusId).flatMap {
      case Some(p) =>
        getAllPermissions(p.id).flatMap {
          case admin: Permission =>
            Future.successful(Some((p, Permissions(Map(admin._1 -> admin._2)))))
          case perms: Seq[Permission] =>
            for {
              pavModulePerms <- getPAVModulePermissions(p.id)
              artifactPerms  <- getArtifactPermissions(p.id)
            } yield {
              val merge1      = pavModulePerms.fold(perms)(perms.prepended)
              val merge2      = artifactPerms.fold(merge1)(merge1.prependedAll)
              val permissions =
                Permissions(merge2.groupBy(_._1).map { case (permType, pos) => (permType, pos.flatMap(_._2).toSet) })
              Some((p, permissions))
            }
        }
      case None => Future.successful(None)
    }
}
