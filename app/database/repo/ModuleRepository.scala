package database.repo

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import cats.data.NonEmptyList
import database.*
import database.table.*
import models.*
import models.core.Specialization
import parsing.types.*
import parsing.types.Module
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

@Singleton
final class ModuleRepository @Inject() (
    val dbConfigProvider: DatabaseConfigProvider,
    private implicit val ctx: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
    with Filterable[ModuleDbEntry, ModuleTable] {
  import profile.api.*

  val tableQuery = TableQuery[ModuleTable]

  private val moduleRelationTable =
    TableQuery[ModuleRelationTable]

  private val moduleResponsibilityTable =
    TableQuery[ModuleResponsibilityTable]

  private val moduleAssessmentMethodTable =
    TableQuery[ModuleAssessmentMethodTable]

  private val modulePOMandatoryTable =
    TableQuery[ModulePOMandatoryTable]

  private val modulePOOptionalTable =
    TableQuery[ModulePOOptionalTable]

  private val moduleTaughtWithTable =
    TableQuery[ModuleTaughtWithTable]

  protected val makeFilter: PartialFunction[(String, String), ModuleTable => Rep[
    Boolean
  ]] = {
    case ("user", value) =>
      t =>
        moduleResponsibilityTable
          .filter(r => r.module === t.id && r.isIdentity(value))
          .exists
    case ("id", value) => _.id === UUID.fromString(value)
    case ("po", value) =>
      t =>
        modulePOMandatoryTable
          .filter(a => a.module === t.id && a.fullPo === value)
          .exists ||
          modulePOOptionalTable
            .filter(a => a.module === t.id && a.fullPo === value)
            .exists
  }

  def createOrUpdateMany(modules: Seq[(Module, LocalDateTime)]) = {
    def createOrUpdateInstant = modules.map {
      case (module, lastModified) =>
        val db = toDbEntry(module, lastModified)
        for {
          exists <- existsAction(module)
          _ <-
            if exists then tableQuery.filter(_.id === module.metadata.id).update(db) else tableQuery += db
        } yield ()
    }
    def dependencies = modules.map {
      case (module, _) =>
        for
          _ <- deleteDependencies(module.metadata.id)
          _ <- createDependencies(module.metadata)
        yield ()
    }

    val actions = DBIO
      .seq(
        DBIO.sequence(createOrUpdateInstant),
        DBIO.sequence(dependencies)
      )
      .transactionally
    db.run(actions)
  }

  def all(filter: Map[String, Seq[String]]) =
    retrieve(allWithFilter(filter))

  def allModuleCore() =
    db.run(
      tableQuery
        .map(m => (m.id, m.title, m.abbrev))
        .result
        .map(_.map(ModuleCore.apply.tupled))
    )

  // TODO the po join does not consider full po id
  def allGenericModulesWithPOs(): Future[Seq[(ModuleCore, Seq[String])]] =
    db.run(
      tableQuery
        .filter(_.moduleType === "generic_module")
        .join(modulePOMandatoryTable)
        .on(_.id === _.module)
        .map { case (m, po) => ((m.id, m.title, m.abbrev), po.po) }
        .result
        .map(
          _.groupBy(_._1)
            .map {
              case (m, pos) =>
                (ModuleCore(m._1, m._2, m._3), pos.map(_._2))
            }
            .toSeq
        )
    )

  // TODO this should be used to fetch modules for po
  def allActiveFromMandatoryPO(po: String | Specialization): Future[Seq[(ModuleProtocol, LocalDateTime)]] = {
    val poFilter: ModuleTable => Rep[Boolean] = po match
      case po: String =>
        t => modulePOMandatoryTable.filter(a => t.id === a.module && a.po === po && t.isActive()).exists
      case Specialization(id, _, po) =>
        t =>
          modulePOMandatoryTable
            .filter(a =>
              t.id === a.module && a.po === po && t.isActive() && a.specialization.map(_ === id).getOrElse(true)
            )
            .exists
    retrieve(tableQuery.filter(poFilter))
  }

  // TODO this should be used to fetch modules for po
  def allFromPO(po: String | Specialization, activeOnly: Boolean): Future[Seq[(ModuleProtocol, LocalDateTime)]] = {
    val poFilter: ModuleTable => Rep[Boolean] = po match
      case po: String =>
        t =>
          modulePOMandatoryTable.filter(a => t.id === a.module && a.po === po).exists ||
            modulePOOptionalTable.filter(a => t.id === a.module && a.po === po).exists
      case Specialization(id, _, po) =>
        t =>
          modulePOMandatoryTable
            .filter(a => t.id === a.module && a.po === po && a.specialization.map(_ === id).getOrElse(true))
            .exists ||
            modulePOOptionalTable
              .filter(a => t.id === a.module && a.po === po && a.specialization.map(_ === id).getOrElse(true))
              .exists
    retrieve(tableQuery.filter(a => if activeOnly then a.isActive() && poFilter(a) else poFilter(a)))
  }

  def deleteDependencies(moduleId: UUID) =
    for {
      _ <- moduleTaughtWithTable.filter(_.module === moduleId).delete
      _ <- modulePOOptionalTable.filter(_.module === moduleId).delete
      _ <- modulePOMandatoryTable.filter(_.module === moduleId).delete
      _ <- moduleAssessmentMethodTable.filter(_.module === moduleId).delete
      _ <- moduleResponsibilityTable.filter(_.module === moduleId).delete
      _ <- moduleRelationTable.filter(_.module === moduleId).delete
    } yield ()

  private def createDependencies(metadata: Metadata) = {
    val methods                   = metadataAssessmentMethods(metadata)
    val (poMandatory, poOptional) = pos(metadata)

    for {
      _ <- moduleRelationTable ++= moduleRelations(metadata)
      _ <- moduleResponsibilityTable ++= responsibilities(metadata)
      _ <- moduleAssessmentMethodTable ++= methods
      _ <- modulePOMandatoryTable ++= poMandatory
      _ <- modulePOOptionalTable ++= poOptional
      _ <- moduleTaughtWithTable ++= metadataTaughtWith(metadata)
    } yield ()
  }

  private def toDbEntry(module: Module, timestamp: LocalDateTime) =
    ModuleDbEntry(
      module.metadata.id,
      timestamp,
      module.metadata.title,
      module.metadata.abbrev,
      module.metadata.kind.id,
      module.metadata.ects.value,
      module.metadata.language.id,
      module.metadata.duration,
      module.metadata.season.id,
      module.metadata.workload,
      module.metadata.status.id,
      module.metadata.location.id,
      Examiner(
        module.metadata.examiner.first.id,
        module.metadata.examiner.second.id
      ),
      module.metadata.examPhases.map(_.id),
      module.metadata.participants,
      module.metadata.prerequisites.recommended.map(ModulePrerequisiteEntry.toProtocol),
      module.metadata.prerequisites.required.map(ModulePrerequisiteEntry.toProtocol),
      module.deContent,
      module.enContent
    )

  private def metadataTaughtWith(metadata: Metadata): List[ModuleTaughtWithDbEntry] =
    metadata.taughtWith.map(m => ModuleTaughtWithDbEntry(metadata.id, m.id))

  private def pos(metadata: Metadata): (List[ModulePOMandatoryDbEntry], List[ModulePOOptionalDbEntry]) =
    (
      metadata.pos.mandatory.map(po =>
        ModulePOMandatoryDbEntry(
          UUID.randomUUID(),
          metadata.id,
          po.po.id,
          po.specialization.map(_.id),
          po.recommendedSemester
        )
      ),
      metadata.pos.optional.map(po =>
        ModulePOOptionalDbEntry(
          UUID.randomUUID(),
          metadata.id,
          po.po.id,
          po.specialization.map(_.id),
          po.instanceOf.id,
          po.partOfCatalog,
          po.recommendedSemester
        )
      )
    )

  private def moduleRelations(metadata: Metadata): List[ModuleRelationDbEntry] =
    metadata.relation match {
      case Some(ModuleRelation.Parent(children)) =>
        children.toList.map(child =>
          ModuleRelationDbEntry(
            metadata.id,
            ModuleRelationType.Parent,
            child.id
          )
        )
      case Some(ModuleRelation.Child(parent)) =>
        List(
          ModuleRelationDbEntry(
            metadata.id,
            ModuleRelationType.Child,
            parent.id
          )
        )
      case None =>
        Nil
    }

  private def responsibilities(metadata: Metadata): List[ModuleResponsibilityDbEntry] = {
    val result = ListBuffer.empty[ModuleResponsibilityDbEntry]
    metadata.responsibilities.lecturers.map(p =>
      result += ModuleResponsibilityDbEntry(
        metadata.id,
        p.id,
        ResponsibilityType.Lecturer
      )
    )
    metadata.responsibilities.moduleManagement.map(p =>
      result += ModuleResponsibilityDbEntry(
        metadata.id,
        p.id,
        ResponsibilityType.ModuleManagement
      )
    )
    result.toList
  }

  private def metadataAssessmentMethods(metadata: Metadata): List[ModuleAssessmentMethodDbEntry] = {
    val metadataAssessmentMethods = ListBuffer[ModuleAssessmentMethodDbEntry]()

    def go(xs: List[ModuleAssessmentMethodEntry]): Unit =
      xs.foreach { m =>
        // this check prevents from adding duplicate values
        if !metadataAssessmentMethods.exists(_.assessmentMethod == m.method.id) then {
          val metadataAssessmentMethod = ModuleAssessmentMethodDbEntry(
            UUID.randomUUID,
            metadata.id,
            m.method.id,
            m.percentage,
            Option.when(m.precondition.nonEmpty)(m.precondition.map(_.id))
          )
          metadataAssessmentMethods += metadataAssessmentMethod
        }
      }

    go(metadata.assessmentMethods.mandatory)
    go(metadata.assessmentMethods.optional) // TODO remove this when the format changes

    metadataAssessmentMethods.toList
  }

  private def existsAction(module: Module) =
    tableQuery.filter(_.id === module.metadata.id).exists.result

  private def retrieve(query: Query[ModuleTable, ModuleDbEntry, Seq]): Future[Seq[(ModuleProtocol, LocalDateTime)]] = {
    val action = query
      .joinLeft(moduleRelationTable)
      .on(_.id === _.module)
      .join(moduleResponsibilityTable)
      .on(_._1.id === _.module)
      .joinLeft(moduleAssessmentMethodTable)
      .on(_._1._1.id === _._1.module)
      .joinLeft(modulePOMandatoryTable)
      .on(_._1._1._1.id === _.module)
      .joinLeft(modulePOOptionalTable)
      .on(_._1._1._1._1.id === _.module)
      .joinLeft(moduleTaughtWithTable)
      .on(_._1._1._1._1._1.id === _.module)
      .result
    db.run(
      action.map(
        _.groupBy(_._1._1._1._1._1._1.id)
          .map {
            case (_, deps) =>
              val module                     = deps.head._1._1._1._1._1._1
              val relations                  = mutable.HashSet[ModuleRelationDbEntry]()
              val moduleManagement           = mutable.HashSet[String]()
              val lecturer                   = mutable.HashSet[String]()
              val mandatoryAssessmentMethods = mutable.HashSet[ModuleAssessmentMethodEntryProtocol]()
              val poMandatory                = mutable.HashSet[ModulePOMandatoryProtocol]()
              val poOptional                 = mutable.HashSet[ModulePOOptionalProtocol]()
              val taughtWith                 = mutable.HashSet[UUID]()

              deps
                .foreach {
                  case ((((((m, mr), r), am), poM), poO), tw) =>
                    assume(module.id == m.id) // just for the sake of safety
                    tw.foreach(taughtWith += _.module)
                    poM.foreach(po =>
                      poMandatory += ModulePOMandatoryProtocol(
                        po.po,
                        po.specialization,
                        po.recommendedSemester
                      )
                    )
                    poO.foreach(po =>
                      poOptional += models.ModulePOOptionalProtocol(
                        po.po,
                        po.specialization,
                        po.instanceOf,
                        po.partOfCatalog,
                        po.recommendedSemester
                      )
                    )
                    mr.foreach(relations += _)
                    r.responsibilityType match {
                      case ResponsibilityType.ModuleManagement => moduleManagement += r.identity
                      case ResponsibilityType.Lecturer         => lecturer += r.identity
                    }
                    am.foreach { am =>
                      mandatoryAssessmentMethods += ModuleAssessmentMethodEntryProtocol(
                        am.assessmentMethod,
                        am.percentage,
                        am.precondition.getOrElse(Nil)
                      )
                    }
                }

              val relation = relations.headOption.map { r =>
                r.relationType match {
                  case ModuleRelationType.Parent =>
                    ModuleRelationProtocol.Parent(NonEmptyList.fromListUnsafe(relations.map(_.relationModule).toList))
                  case ModuleRelationType.Child =>
                    ModuleRelationProtocol.Child(r.relationModule)
                }
              }
              (
                ModuleProtocol(
                  Some(module.id),
                  MetadataProtocol(
                    module.title,
                    module.abbrev,
                    module.moduleType,
                    module.ects,
                    module.language,
                    module.duration,
                    module.season,
                    module.workload,
                    module.status,
                    module.location,
                    module.participants,
                    relation,
                    NonEmptyList.fromListUnsafe(moduleManagement.toList),
                    NonEmptyList.fromListUnsafe(lecturer.toList),
                    ModuleAssessmentMethodsProtocol(mandatoryAssessmentMethods.toList, Nil),
                    module.examiner,
                    module.examPhases,
                    ModulePrerequisitesProtocol(module.recommendedPrerequisites, module.requiredPrerequisites),
                    ModulePOProtocol(poMandatory.toList, poOptional.toList),
                    Nil,
                    Nil,
                    taughtWith.toList,
                  ),
                  module.deContent,
                  module.enContent
                ),
                module.lastModified
              )
          }
          .toSeq
      )
    )
  }
}
