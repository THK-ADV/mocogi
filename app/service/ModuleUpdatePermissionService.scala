package service

import database.repo.ModuleUpdatePermissionRepository
import models.ModuleUpdatePermissionType.{Granted, Inherited}
import models.core.Person
import models.{Module, ModuleUpdatePermission, User}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
final class ModuleUpdatePermissionService @Inject() (
    private val repo: ModuleUpdatePermissionRepository,
    private implicit val ctx: ExecutionContext
) {
  def createOrUpdateInherited(modules: Seq[(UUID, List[Person])]) = {
    def entries() =
      modules.flatMap { case (module, management) =>
        management.collect {
          case p: Person.Default if p.campusId != "unknown" =>
            (module, User(p.campusId), Inherited)
        }
      }
    for {
      _ <- repo.deleteByModules(modules.map(_._1), Inherited)
      created <- repo.createMany(entries().toList)
    } yield created
  }

  def createGranted(module: UUID, user: User) =
    repo.create((module, user, Granted))

  def removeGranted(module: UUID, user: User) =
    repo.delete(module, user, Granted)

  def hasPermission(user: User, module: UUID) =
    repo.hasPermission(user, module)

  def getAll() =
    repo
      .allWithModule()
      .map(_.map(ModuleUpdatePermission.tupled))

  def getAllModulesFromUser(user: User): Future[Seq[Module]] =
    repo.allForUser(user)
}
