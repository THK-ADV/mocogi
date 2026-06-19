package database.repo.schedule

import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.mutable
import scala.concurrent.ExecutionContext

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import service.ModuleService
import models.MetadataProtocol

@Singleton
final class ScheduleEntryBootstrapRepository @Inject() (
    moduleService: ModuleService,
    moduleTeachingUnitRepository: ModuleTeachingUnitRepository,
    val dbConfigProvider: DatabaseConfigProvider,
    implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {

  // TODO: This is only used to bootstrap module teaching units associations
  def bootstrapModuleTeachingUnit() =
    for {
      modules <- moduleService.allMetadata()
      entries = modules.map {
        case (id, m) =>
          val pos = mutable.Set[String]()
          m.po.mandatory.foreach(po => pos.add(po.po))
          m.po.optional.foreach(po => pos.add(po.po))
          (id.get, pos.toList)
      }
      _ <- moduleTeachingUnitRepository.update(entries)
    } yield ()
}
